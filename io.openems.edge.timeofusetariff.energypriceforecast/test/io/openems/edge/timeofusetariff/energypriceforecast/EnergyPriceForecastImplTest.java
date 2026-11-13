package io.openems.edge.timeofusetariff.energypriceforecast;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.osgi.service.event.Event;

import com.google.common.collect.ImmutableSortedMap;
import com.google.gson.JsonObject;

import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.dummy.DummyBridgeHttpExecutor;
import io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory;
import io.openems.common.bridge.http.dummy.DummyEndpointFetcher;
import io.openems.common.services.currency.DummyCurrencyExchange;
import io.openems.common.tariff.api.TariffUpdateStatus;
import io.openems.common.test.TimeLeapClock;
import io.openems.common.types.BiddingZone;
import io.openems.common.types.HttpStatus;
import io.openems.common.types.TariffData;
import io.openems.common.types.TariffType;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;

class EnergyPriceForecastImplTest {
	private final Instant start = Instant.parse("2026-11-13T08:00:00Z");
	private final TimeLeapClock clock = new TimeLeapClock(this.start, ZoneId.of("Europe/Berlin"));
	private final DummyEndpointFetcher fetcher = new DummyEndpointFetcher();
	private final DummyBridgeHttpExecutor executor = new DummyBridgeHttpExecutor(this.clock, true);
	private EnergyPriceForecastImpl sut;
	private ComponentTest componentTest;
	private JsonObject response;

	@BeforeEach
	void setUp() throws Exception {
		this.response = JsonUtils.parseToJsonObject(RESPONSE);
		this.sut = new EnergyPriceForecastImpl();
		this.componentTest = new ComponentTest(this.sut) //
				.addReference("componentManager", new DummyComponentManager(this.clock)) //
				.addReference("bridgeHttpFactory",
						DummyBridgeHttpFactory.ofBridgeImpl(() -> this.fetcher, () -> this.executor)) //
				.activate(MyConfig.create().build());
	}

	@AfterEach
	void tearDown() throws Exception {
		this.componentTest.deactivate();
	}

	@Test
	void readsForecastQuartersWithoutFillingGapsOrChangingNegativePrices() throws Exception {
		this.clock.leap(7, MINUTES);
		this.fetcher.addEndpointHandler(endpoint -> {
			assertTrue(endpoint.url().contains("mode=forecast_only"));
			assertTrue(endpoint.url().contains("hours=48"));
			assertTrue(endpoint.url().contains("country=DE"));
			return HttpResponse.ok(this.response.toString());
		});
		var tariff = this.sut.getSpotTariff(BiddingZone.GERMANY);
		this.executor.update();

		var data = tariff.getTariffData().getValue();
		assertNotNull(data);
		assertEquals(-10.0, data.prices().getAt(this.start), 0.000001);
		assertNull(data.prices().getAt(this.start.plus(15, MINUTES)));
		assertEquals(0.0, data.prices().getAt(this.start.plus(30, MINUTES)), 0.000001);
		assertEquals(150.0, data.prices().getAt(this.start.plus(47, HOURS)), 0.000001);
		assertNull(data.prices().getAt(this.start.plus(48, HOURS)));
		assertEquals(Currency.getInstance("EUR"), data.currency());
		assertEquals(this.start, data.lastUpdate());
		assertTrue(data.copyright().contains("Energy Price Forecast EU"));
		assertInstanceOf(TariffUpdateStatus.Successful.class, tariff.getLastUpdateStatus().getValue());
	}

	@Test
	void forecastExtendsPublishedPricesAndCanBeReplacedByNewPublishedPrices() throws Exception {
		this.fetcher.addEndpointHandler(endpoint -> HttpResponse.ok(this.response.toString()));
		var tariff = this.sut.getSpotTariff(BiddingZone.GERMANY);
		this.executor.update();
		var forecast = tariff.getTariffData().getValue();
		var published = new TariffData(new TariffType.DayAheadEpexSpot(BiddingZone.GERMANY),
				TimeOfUsePrices.from(ImmutableSortedMap.of(this.start, 80.0)), Currency.getInstance("EUR"), this.start,
				this.start, "Published source");
		var currencyExchange = new DummyCurrencyExchange();
		var combined = TariffData.byCombination(List.of(published, forecast), published.currency(), currencyExchange);

		assertTrue(this.sut.getRanking() < 80);
		assertEquals(80.0, combined.prices().getAt(this.start), 0.000001);
		assertEquals(150.0, combined.prices().getAt(this.start.plus(47, HOURS)), 0.000001);
		assertNull(combined.prices().getAt(this.start.plus(15, MINUTES)));

		var newlyPublished = new TariffData(published.tariffType(),
				TimeOfUsePrices.from(ImmutableSortedMap.of(this.start, 80.0, this.start.plus(47, HOURS), 120.0)),
				published.currency(), this.start, this.start, published.copyright());
		var updated = TariffData.byCombination(List.of(newlyPublished, forecast), published.currency(),
				currencyExchange);
		assertEquals(120.0, updated.prices().getAt(this.start.plus(47, HOURS)), 0.000001);
	}

	@Test
	void refreshesTheSharedForecastWithANewModelIssue() throws Exception {
		this.fetcher.addEndpointHandler(endpoint -> HttpResponse.ok(this.response.toString()));
		var tariff = this.sut.getSpotTariff(BiddingZone.GERMANY);
		this.executor.update();
		assertEquals(0.0, tariff.getTariffData().getValue().prices().getAt(this.start.plus(30, MINUTES)), 0.000001);

		this.clock.leap(30, MINUTES);
		this.response.getAsJsonArray("entries").get(1).getAsJsonObject().addProperty("value", 0.05);
		this.response.getAsJsonObject("source").getAsJsonObject("firestore").addProperty("updated_at",
				this.clock.instant().toString());
		this.executor.update();
		assertEquals(50.0, tariff.getTariffData().getValue().prices().getAt(this.clock.instant()), 0.000001);
		assertEquals(this.clock.instant(), tariff.getTariffData().getValue().lastUpdate());
	}

	@Test
	void sharesOneRequestBetweenConsumersAndStopsAfterLastConsumerLeaves() throws Exception {
		var calls = new AtomicInteger();
		this.fetcher.addEndpointHandler(endpoint -> {
			calls.incrementAndGet();
			return HttpResponse.ok(this.response.toString());
		});
		var first = this.sut.getSpotTariff(BiddingZone.GERMANY);
		var second = this.sut.getSpotTariff(BiddingZone.GERMANY);
		assertSame(first, second);
		this.executor.update();
		assertEquals(1, calls.get());
		assertEquals(1, this.sut.getInfosAboutLoadedTariffs().getValue().size());

		this.sut.ungetSpotTariff(first);
		assertNotNull(second.getTariffData().getValue());
		this.sut.ungetSpotTariff(second);
		this.clock.leap(31, MINUTES);
		this.executor.update();
		assertEquals(1, calls.get());
		assertTrue(this.sut.getInfosAboutLoadedTariffs().getValue().isEmpty());
	}

	@Test
	void releasesTheLastConsumerWhileAnHttpCallbackIsPublishing() throws Exception {
		this.fetcher.addEndpointHandler(endpoint -> HttpResponse.ok(this.response.toString()));
		var tariff = this.sut.getSpotTariff(BiddingZone.GERMANY);
		var publishing = new CompletableFuture<Void>();
		var finishPublishing = new CompletableFuture<Void>();
		tariff.getTariffData().subscribe(data -> {
			if (data != null) {
				publishing.complete(null);
				finishPublishing.join();
			}
		});
		var worker = Thread.ofVirtual().start(this.executor::update);
		try {
			publishing.get(5, TimeUnit.SECONDS);
			assertTimeoutPreemptively(Duration.ofSeconds(2), () -> this.sut.ungetSpotTariff(tariff));
		} finally {
			finishPublishing.complete(null);
			worker.join(5000);
		}
		assertFalse(worker.isAlive());
		assertTrue(this.sut.getInfosAboutLoadedTariffs().getValue().isEmpty());
	}

	@Test
	void retainsFreshForecastDuringOutageButExpiresItBySourceTimestamp() throws Exception {
		this.fetcher.addSingleUseEndpointHandler(endpoint -> HttpResponse.ok(this.response.toString()));
		this.fetcher.addEndpointHandler(endpoint -> new HttpResponse<>(HttpStatus.SERVICE_UNAVAILABLE, "Unavailable"));
		var tariff = this.sut.getSpotTariff(BiddingZone.GERMANY);
		this.executor.update();
		var initial = tariff.getTariffData().getValue();
		this.clock.leap(30, MINUTES);
		this.executor.update();
		assertSame(initial, tariff.getTariffData().getValue());
		assertInstanceOf(TariffUpdateStatus.FailedWithHttpError.class, tariff.getLastUpdateStatus().getValue());

		this.clock.leap(24, HOURS);
		this.sut.handleEvent(new Event(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, Map.of()));
		assertNull(tariff.getTariffData().getValue());
		assertInstanceOf(TariffUpdateStatus.FailedWithException.class, tariff.getLastUpdateStatus().getValue());
	}

	@Test
	void recoversAfterInitialHttpFailure() throws Exception {
		this.fetcher
				.addSingleUseEndpointHandler(endpoint -> new HttpResponse<>(HttpStatus.SERVICE_UNAVAILABLE, "Down"));
		this.fetcher.addEndpointHandler(endpoint -> HttpResponse.ok(this.response.toString()));
		var tariff = this.sut.getSpotTariff(BiddingZone.GERMANY);
		this.executor.update();
		assertNull(tariff.getTariffData().getValue());
		this.clock.leap(5, MINUTES);
		this.executor.update();
		assertNotNull(tariff.getTariffData().getValue());
		assertInstanceOf(TariffUpdateStatus.Successful.class, tariff.getLastUpdateStatus().getValue());
	}

	@ParameterizedTest
	@ValueSource(strings = { "unit", "country", "mode", "stale", "interval", "duplicate", "empty" })
	void rejectsUnusableResponsesWithoutInventingPrices(String invalidField) throws Exception {
		var response = this.response.deepCopy();
		switch (invalidField) {
		case "unit" -> response.addProperty("unit", "EUR/MWh");
		case "country" -> response.addProperty("country", "NL");
		case "mode" -> response.getAsJsonObject("source").addProperty("mode", "mixed");
		case "stale" -> response.getAsJsonObject("source").getAsJsonObject("firestore").addProperty("updated_at",
				this.start.minus(25, HOURS).toString());
		case "interval" -> response.getAsJsonArray("entries").get(0).getAsJsonObject().addProperty("end",
				this.start.plus(60, MINUTES).toString());
		case "duplicate" -> response.getAsJsonArray("entries").add(response.getAsJsonArray("entries").get(0));
		case "empty" -> response.getAsJsonArray("entries").asList().clear();
		default -> throw new IllegalArgumentException(invalidField);
		}
		this.fetcher.addEndpointHandler(endpoint -> HttpResponse.ok(response.toString()));
		var tariff = this.sut.getSpotTariff(BiddingZone.GERMANY);
		this.executor.update();
		assertNull(tariff.getTariffData().getValue());
		assertInstanceOf(TariffUpdateStatus.FailedWithException.class, tariff.getLastUpdateStatus().getValue());
	}

	@ParameterizedTest
	@CsvSource({ "2026-03-29T00:00:00Z,2026-03-29T01:00:00Z", "2026-10-25T00:00:00Z,2026-10-25T01:00:00Z" })
	void keepsUtcIntervalsDistinctAcrossClockChanges(String first, String second) throws Exception {
		var firstInstant = Instant.parse(first);
		this.clock.leap(firstInstant.getEpochSecond() - this.clock.instant().getEpochSecond(),
				java.time.temporal.ChronoUnit.SECONDS);
		var response = this.response.deepCopy();
		response.getAsJsonObject("source").getAsJsonObject("firestore").addProperty("updated_at", first);
		response.add("entries", JsonUtils.parseToJsonArray("""
				[{"start":"%s","end":"%s","value":0.1},
				 {"start":"%s","end":"%s","value":0.2}]
				""".formatted(first, firstInstant.plus(15, MINUTES), second, Instant.parse(second).plus(15, MINUTES))));
		this.fetcher.addEndpointHandler(endpoint -> HttpResponse.ok(response.toString()));
		var tariff = this.sut.getSpotTariff(BiddingZone.GERMANY);
		this.executor.update();
		var prices = tariff.getTariffData().getValue().prices();
		assertEquals(100.0, prices.getAt(firstInstant), 0.000001);
		assertEquals(200.0, prices.getAt(Instant.parse(second)), 0.000001);
	}

	@Test
	void onlyAdvertisesSupportedMarkets() {
		assertTrue(this.sut.isSpotTariffSupported(BiddingZone.GERMANY));
		assertTrue(this.sut.isSpotTariffSupported(BiddingZone.SWEDEN_SE3));
		assertFalse(this.sut.isSpotTariffSupported(BiddingZone.LITHUANIA));
		assertThrows(IllegalArgumentException.class, () -> this.sut.getSpotTariff(BiddingZone.LITHUANIA));
	}

	private static final String RESPONSE = """
			{"country":"DE","currency":"EUR","unit":"EUR/kWh",
			 "source":{"mode":"forecast_only","firestore":{"updated_at":"2026-11-13T08:00:00Z"}},
			 "entries":[
			   {"start":"2026-11-13T08:00:00Z","end":"2026-11-13T08:15:00Z","value":-0.01},
			   {"start":"2026-11-13T08:30:00Z","end":"2026-11-13T08:45:00Z","value":0.0},
			   {"start":"2026-11-15T07:00:00Z","end":"2026-11-15T07:15:00Z","value":0.15},
			   {"start":"2026-11-15T08:00:00Z","end":"2026-11-15T08:15:00Z","value":0.25}
			 ]}
			""";
}
