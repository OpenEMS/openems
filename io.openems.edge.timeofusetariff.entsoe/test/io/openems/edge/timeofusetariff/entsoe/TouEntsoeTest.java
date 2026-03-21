package io.openems.edge.timeofusetariff.entsoe;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.common.utils.JsonUtils.buildJsonArray;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.common.utils.JsonUtils.parseToJsonArray;
import static io.openems.edge.common.currency.Currency.EUR;
import static io.openems.edge.timeofusetariff.entsoe.Utils.parseToSchedule;
import static java.time.LocalTime.MIN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;

import org.junit.Test;

import com.google.common.io.Resources;

import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.dummy.DummyBridgeHttpBundle;
import io.openems.common.bridge.http.time.periodic.DummyPeriodicExecutorFactory;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.oem.DummyOpenemsEdgeOem;
import io.openems.common.test.TimeLeapClock;
import io.openems.common.types.EntsoeBiddingZone;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyMeta;
import io.openems.edge.timeofusetariff.api.AncillaryCosts.GridFee;
import io.openems.edge.timeofusetariff.api.GermanDSO;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;
import io.openems.edge.timeofusetariff.api.TouManualHelper;
import io.openems.edge.timeofusetariff.entsoe.priceprovider.EntsoeMarketPriceProviderPoolImpl;

public class TouEntsoeTest {
	private static final long FULL_DAY_MINUTES = 1440;
	private static final ZoneId GERMAN_ZONE_ID = ZoneId.of("Europe/Berlin");
	private static final ZoneId ZONE_ID = ZoneId.systemDefault();
	private static final int YEAR = 2026;

	private static String SCHEDULE = """
			[
			  {
			    "year": 2026,
			    "tariffs": {
			      "low": 0.10,
			      "standard": 0.20,
			      "high": 0.30
			    },
			    "quarters": [
			      {
			        "quarter": 1,
			        "dailySchedule": [
			          { "tariff": "low", "from": "00:00", "to": "06:00" },
			          { "tariff": "standard", "from": "06:00", "to": "18:00" },
			          { "tariff": "high", "from": "18:00", "to": "23:59" }
			        ]
			      },
				  {
			        "quarter": 2,
			        "dailySchedule": [ ]
			      },
				  {
			        "quarter": 3,
			        "dailySchedule": [ ]
			      },
				  {
			        "quarter": 4,
			        "dailySchedule": [
			          { "tariff": "low", "from": "00:00", "to": "06:00" },
			          { "tariff": "standard", "from": "06:00", "to": "18:00" },
			          { "tariff": "high", "from": "18:00", "to": "23:59" }
			        ]
			      }
			    ]
			  }
			]
			""";

	private EntsoeMarketPriceProviderPoolImpl createPool(BridgeHttpFactory httpBridgeFactory) {
		final var clock = createDummyClock();
		final var dummyOem = new DummyOpenemsEdgeOem();

		var dummyClockProvider = new ClockProvider() {
			@Override
			public Clock getClock() {
				return clock;
			}
		};

		return new EntsoeMarketPriceProviderPoolImpl(//
				dummyOem, //
				dummyClockProvider, //
				httpBridgeFactory, //
				new DummyPeriodicExecutorFactory() //
		) {
		};
	}

	@Test
	public void testHttpFetch() throws Exception {
		final var httpTestBundle = new DummyBridgeHttpBundle();
		final var clock = new TimeLeapClock(Instant.parse("2026-02-01T23:00:00Z"));
		final var entsoe = new TouEntsoeImpl();
		var dummyMeta = new DummyMeta() //
				.withCurrency(EUR);

		var pool = this.createPool(httpTestBundle.factory());

		new ComponentTest(entsoe) //
				.addReference("meta", dummyMeta) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("entsoeMarketPriceProviderPool", pool) //
				.activate(MyConfig.create() //
						.setId("tou0") //
						.setSecurityToken("Unit-Test") //
						.setBiddingZone(EntsoeBiddingZone.GERMANY) //
						.setAncillaryCosts(buildJsonObject() //
								.addProperty("dso", "BAYERNWERK") //
								.build() //
								.toString())
						.build())

				.next(new TestCase("Successful response") //
						.onBeforeProcessImage(() -> {
							var testResponse = this.getTestEntsoeResponse();
							httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok(testResponse));
							httpTestBundle.runTasksImmediately();
						}) //
						.onAfterProcessImage(() -> {
							assertNotEquals(TimeOfUsePrices.EMPTY_PRICES, entsoe.getPrices());
						}) //
						.output(TouEntsoe.ChannelId.HTTP_STATUS_CODE, 200) //
						.output(TouEntsoe.ChannelId.UNABLE_TO_UPDATE_PRICES, false) //
				) //
				.next(new TestCase("Failed response") //
						.onBeforeProcessImage(() -> {
							entsoe.triggerPriceUpdate();
							httpTestBundle.forceNextFailedResult(HttpError.ResponseError.notFound());
							httpTestBundle.runTasksImmediately();
						}) //
						.onAfterProcessImage(() -> {
							// Prices should not be changed on failed update
							assertNotEquals(TimeOfUsePrices.EMPTY_PRICES, entsoe.getPrices());
						}) //
						.output(TouEntsoe.ChannelId.HTTP_STATUS_CODE, 404) //
						.output(TouEntsoe.ChannelId.UNABLE_TO_UPDATE_PRICES, true) //
				) //
		;
	}

	private String getTestEntsoeResponse() throws IOException {
		var resource = TouEntsoeTest.class.getResource("entsoe-response.xml");
		assertNotNull(resource);
		return Resources.toString(resource, StandardCharsets.UTF_8);
	}

	@Test
	public void testAllDsoSchedules() {
		for (final var dso : GermanDSO.values()) {
			final var gridFee = dso.gridFee;
			for (final var dateRange : gridFee.dateRanges()) {
				var currentDay = dateRange.start();
				while (!currentDay.isAfter(dateRange.end())) {
					// Sort time ranges by start time
					final var timeRanges = dateRange.timeRanges() //
							.stream() //
							.sorted(Comparator.comparing(GridFee.TimeRange::start)) //
							.toList();

					// Check coverage and overlaps
					var lastEnd = MIN;
					long totalMinutes = 0;

					for (final var tr : timeRanges) {
						final var start = tr.start();
						final var end = tr.end();

						// Check for gaps or overlaps
						assertTrue("[" + dso.name() + "] Gap/overlap on " + currentDay + " between " + lastEnd + " and "
								+ start, !lastEnd.isAfter(start));

						// Calculate duration in minutes
						totalMinutes += calculateMinutes(start, end);
						lastEnd = end;
					}

					// Verify entire day is covered (24 hours)
					assertTrue("[" + dso.name() + "] Incomplete day coverage on " + currentDay + ": " + totalMinutes
							+ " minutes", totalMinutes == FULL_DAY_MINUTES);

					// Verify tariff consistency: low < standard < high
					if (dateRange.lowTariff() > 0 || dateRange.highTariff() > 0) {
						assertTrue("[" + dso.name() + "] Tariff violation: low < standard < high",
								dateRange.lowTariff() < dateRange.standardTariff()
										&& dateRange.standardTariff() < dateRange.highTariff());
					}
					currentDay = currentDay.plusDays(1);
				}
			}
		}
	}

	private static long calculateMinutes(LocalTime start, LocalTime end) {

		final var difference = Duration.between(start, end);

		if (!difference.isPositive()) {
			return difference.plusDays(1).toMinutes();
		}
		return difference.toMinutes();
	}

	private TouManualHelper buildHelper(String dso, String clockTime) throws OpenemsNamedException {
		var ancillaryCosts = buildJsonObject() //
				.addProperty("dso", dso) //
				.build() //
				.toString();

		var clock = new TimeLeapClock(Instant.parse(clockTime), GERMAN_ZONE_ID);
		var schedule = parseToSchedule(clock, EntsoeBiddingZone.GERMANY, ancillaryCosts, msg -> fail(msg));

		return new TouManualHelper(clock, schedule, 0.0);
	}

	private static ZonedDateTime toZonedDateTime(int year, int month, int day, int hour, int minute) {
		return LocalDate.of(year, month, day).atTime(hour, minute).atZone(ZONE_ID);
	}

	@Test
	public void testStandardTariffOnJuly15At2PM() throws OpenemsNamedException {
		var helper = this.buildHelper("BAYERNWERK", YEAR + "-07-15T00:00:00Z");
		final var testTime = toZonedDateTime(YEAR, 7, 15, 14, 0);
		var expectedPrice = GermanDSO.BAYERNWERK.getPriceAt(testTime);

		assertEquals(expectedPrice, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("NETZE_ODR", YEAR + "-07-15T00:00:00Z");

		expectedPrice = GermanDSO.NETZE_ODR.getPriceAt(testTime);

		assertEquals(expectedPrice, helper.getPrices().getAt(testTime), 0.01);
	}

	@Test
	public void testLowTariffOnOctober20At3AM() throws OpenemsNamedException {
		var helper = this.buildHelper("BAYERNWERK", YEAR + "-10-20T00:00:00Z");
		final var testTime = toZonedDateTime(YEAR, 10, 20, 3, 0);
		var expectedPrice = GermanDSO.BAYERNWERK.getPriceAt(testTime);

		assertEquals(expectedPrice, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("NETZE_BW", YEAR + "-10-20T00:00:00Z");
		expectedPrice = GermanDSO.NETZE_BW.getPriceAt(testTime);

		assertEquals(expectedPrice, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("MIT_NETZ", YEAR + "-10-20T00:00:00Z");
		expectedPrice = GermanDSO.MIT_NETZ.getPriceAt(testTime);

		assertEquals(expectedPrice, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("E_DIS", YEAR + "-10-20T00:00:00Z");
		expectedPrice = GermanDSO.E_DIS.getPriceAt(testTime);

		assertEquals(expectedPrice, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("LEW", YEAR + "-10-20T00:00:00Z");
		expectedPrice = GermanDSO.LEW.getPriceAt(testTime);

		assertEquals(expectedPrice, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("NETZE_ODR", YEAR + "-10-20T00:00:00Z");
		expectedPrice = GermanDSO.NETZE_ODR.getPriceAt(testTime);

		assertEquals(expectedPrice, helper.getPrices().getAt(testTime), 0.01);
	}

	@Test
	public void testHighTariffOnOctober20At5PM() throws OpenemsNamedException {
		var helper = this.buildHelper("BAYERNWERK", YEAR + "-10-20T00:00:00Z");
		final var testTime = toZonedDateTime(YEAR, 10, 20, 17, 0);
		var expectedPrice = GermanDSO.BAYERNWERK.getPriceAt(testTime);

		assertEquals(expectedPrice, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("NETZE_BW", YEAR + "-10-20T00:00:00Z");
		expectedPrice = GermanDSO.NETZE_BW.getPriceAt(testTime);

		assertEquals(expectedPrice, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("MIT_NETZ", YEAR + "-10-20T00:00:00Z");
		expectedPrice = GermanDSO.MIT_NETZ.getPriceAt(testTime);

		assertEquals(expectedPrice, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("WEST_NETZ", YEAR + "-10-20T00:00:00Z");
		expectedPrice = GermanDSO.WEST_NETZ.getPriceAt(testTime);

		assertEquals(expectedPrice, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("AVACON", YEAR + "-10-20T00:00:00Z");
		expectedPrice = GermanDSO.AVACON.getPriceAt(testTime);

		assertEquals(expectedPrice, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("NETZE_ODR", YEAR + "-10-20T00:00:00Z");
		expectedPrice = GermanDSO.NETZE_ODR.getPriceAt(testTime);

		assertEquals(expectedPrice, helper.getPrices().getAt(testTime), 0.01);
	}

	@Test
	public void testStandardTariffOnApril10At12PM() throws OpenemsNamedException {
		var helper = this.buildHelper("BAYERNWERK", YEAR + "-04-10T00:00:00Z");
		var testTime = toZonedDateTime(YEAR, 4, 10, 12, 0);
		var expectedPrice = GermanDSO.BAYERNWERK.getPriceAt(testTime);

		assertEquals(expectedPrice, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("NETZE_BW", YEAR + "-04-10T00:00:00Z");
		expectedPrice = GermanDSO.NETZE_BW.getPriceAt(testTime);

		assertEquals(expectedPrice, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("NETZE_BW", YEAR + "-04-09T00:00:00Z");
		testTime = toZonedDateTime(YEAR, 4, 10, 0, 0);
		expectedPrice = GermanDSO.NETZE_BW.getPriceAt(testTime);

		assertEquals(expectedPrice, helper.getPrices().getAt(testTime), 0.01);

		helper = this.buildHelper("NETZE_ODR", YEAR + "-04-09T00:00:00Z");
		expectedPrice = GermanDSO.NETZE_ODR.getPriceAt(testTime);

		assertEquals(expectedPrice, helper.getPrices().getAt(testTime), 0.01);
	}

	@Test
	public void testEmptyScheduleReturnsZeroPrice() throws OpenemsNamedException {
		var clock = new TimeLeapClock(Instant.parse(YEAR + "-02-01T00:00:00Z"), GERMAN_ZONE_ID);
		var ancillaryCosts = buildJsonObject() //
				.addProperty("dso", "other") //
				.add("schedule", buildJsonArray() //
						.build()) // empty
				.build() //
				.toString();

		var schedule = parseToSchedule(clock, EntsoeBiddingZone.GERMANY, ancillaryCosts, msg -> fail(msg));
		var helper = new TouManualHelper(clock, schedule, 0.0);

		var testTime = toZonedDateTime(YEAR, 2, 1, 10, 0);
		assertEquals(0.0, helper.getPrices().getAt(testTime), 0.01);
	}

	@Test
	public void testMissingSchedule() throws OpenemsNamedException {
		var clock = new TimeLeapClock(Instant.parse(YEAR + "-02-01T00:00:00Z"), GERMAN_ZONE_ID);
		var ancillaryCosts = buildJsonObject() //
				.addProperty("dso", "other") // simulate missing schedule
				.build() //
				.toString();

		var schedule = parseToSchedule(clock, EntsoeBiddingZone.GERMANY, ancillaryCosts, msg -> fail(msg));
		var helper = new TouManualHelper(clock, schedule, 0.0);

		var testTime = toZonedDateTime(YEAR, 2, 1, 10, 0);
		assertEquals(0.0, helper.getPrices().getAt(testTime), 0.01);
	}

	@Test
	public void testSchedule() throws Exception {
		final var httpTestBundle = new DummyBridgeHttpBundle();
		final var clock = new TimeLeapClock(Instant.parse("2026-02-02T00:00:00Z"));
		var entsoe = new TouEntsoeImpl();
		var dummyMeta = new DummyMeta() //
				.withCurrency(EUR);

		var pool = this.createPool(httpTestBundle.factory());

		var schedule = parseToJsonArray(SCHEDULE);
		new ComponentTest(entsoe) //
				.addReference("meta", dummyMeta) //
				.addReference("entsoeMarketPriceProviderPool", pool) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.activate(MyConfig.create() //
						.setId("tou0") //
						.setSecurityToken("TEST") //
						.setBiddingZone(EntsoeBiddingZone.GERMANY) //
						.setAncillaryCosts(buildJsonObject() //
								.addProperty("dso", "OTHER") //
								.add("schedule", schedule) //
								.build() //
								.toString()) //
						.build());

		httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok(this.getTestEntsoeResponse()));
		httpTestBundle.runTasksImmediately();

		assertEquals(99.41 + (0.1 * 10), entsoe.getPrices().getAt(Instant.parse("2026-02-02T00:00:00Z")), 0.001);
		assertEquals(95.00 + (0.1 * 10), entsoe.getPrices().getAt(Instant.parse("2026-02-02T00:15:00Z")), 0.001);
		assertEquals(112.97 + (0.2 * 10), entsoe.getPrices().getAt(Instant.parse("2026-02-02T06:00:00Z")), 0.001);
		assertEquals(97.41 + (0.3 * 10), entsoe.getPrices().getAt(Instant.parse("2026-02-02T23:00:00Z")), 0.001);
	}

	@Test
	public void testSchedulePrices() throws OpenemsNamedException {
		var customSchedule = parseToJsonArray(SCHEDULE);
		var ancillaryCosts = buildJsonObject() //
				.addProperty("dso", "OTHER") //
				.add("schedule", customSchedule) //
				.build() //
				.toString();

		var clock = new TimeLeapClock(Instant.parse("2026-03-01T00:00:00Z"), ZoneId.systemDefault());
		var schedule = parseToSchedule(clock, EntsoeBiddingZone.GERMANY, ancillaryCosts, System.out::println);
		var helper = new TouManualHelper(clock, schedule, 0.0);

		// Validate prices
		var testTime1 = ZonedDateTime.of(LocalDate.of(2026, 3, 1), LocalTime.of(5, 0), ZoneId.systemDefault());
		assertEquals(0.1, helper.getPrices().getAt(testTime1), 0.01);

		var testTime2 = ZonedDateTime.of(LocalDate.of(2026, 3, 1), LocalTime.of(12, 0), ZoneId.systemDefault());
		assertEquals(0.2, helper.getPrices().getAt(testTime2), 0.01);

		var testTime3 = ZonedDateTime.of(LocalDate.of(2026, 3, 1), LocalTime.of(23, 0), ZoneId.systemDefault());
		assertEquals(0.3, helper.getPrices().getAt(testTime3), 0.01);

		// Test Standard tariff if timerange not given.
		clock = new TimeLeapClock(Instant.parse("2026-06-01T00:00:00Z"), ZoneId.systemDefault());
		helper = new TouManualHelper(clock, schedule, 0.0);

		var testTime4 = ZonedDateTime.of(LocalDate.of(2026, 6, 1), LocalTime.of(23, 0), ZoneId.systemDefault());
		assertEquals(0.2, helper.getPrices().getAt(testTime4), 0.01);

		clock = new TimeLeapClock(Instant.parse("2026-07-01T00:00:00Z"), ZoneId.systemDefault());
		helper = new TouManualHelper(clock, schedule, 0.0);

		var testTime5 = ZonedDateTime.of(LocalDate.of(2026, 7, 1), LocalTime.of(10, 0), ZoneId.systemDefault());
		assertEquals(0.2, helper.getPrices().getAt(testTime5), 0.01);

		clock = new TimeLeapClock(Instant.parse("2026-11-01T00:00:00Z"), ZoneId.systemDefault());
		helper = new TouManualHelper(clock, schedule, 0.0);

		var testTime6 = ZonedDateTime.of(LocalDate.of(2026, 11, 1), LocalTime.of(23, 0), ZoneId.systemDefault());
		assertEquals(0.3, helper.getPrices().getAt(testTime6), 0.01);
	}
}
