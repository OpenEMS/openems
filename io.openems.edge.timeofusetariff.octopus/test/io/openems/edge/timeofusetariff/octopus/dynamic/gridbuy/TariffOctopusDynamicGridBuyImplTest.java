package io.openems.edge.timeofusetariff.octopus.dynamic.gridbuy;

import static io.openems.edge.common.currency.Currency.EUR;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import org.junit.jupiter.api.Test;

import io.openems.common.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.dummy.DummyBridgeHttpExecutor;
import io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory;
import io.openems.common.bridge.http.dummy.DummyEndpointFetcher;
import io.openems.common.test.TimeLeapClock;
import io.openems.common.types.HttpStatus;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyMeta;

/**
 * Offline component tests. All HTTP requests are answered by a
 * {@link DummyEndpointFetcher}; no network access or real account data is used.
 */
class TariffOctopusDynamicGridBuyImplTest {

	private static final String VALID_FORECAST = Data.forecast(Data.agreement(//
			Data.slot("2026-10-11T10:00:00Z", "2026-10-11T10:15:00Z", "21.5"), //
			Data.slot("2026-10-11T10:15:00Z", "2026-10-11T10:30:00Z", "18.25"), //
			Data.slot("2026-10-11T10:30:00Z", "2026-10-11T10:45:00Z", "20"), //
			Data.slot("2026-10-11T10:45:00Z", "2026-10-11T11:00:00Z", "22")));

	/**
	 * Simulated Kraken API. Answers token requests with a fresh token by default
	 * and price requests from a queue.
	 */
	private static final class FakeKraken {

		private final TimeLeapClock clock = new TimeLeapClock(Instant.parse("2026-10-11T10:05:00Z"), ZoneOffset.UTC);
		private final DummyEndpointFetcher fetcher = DummyBridgeHttpFactory.dummyEndpointFetcher();
		private final DummyBridgeHttpExecutor executor = DummyBridgeHttpFactory.dummyBridgeHttpExecutor(this.clock,
				true);
		private final Queue<Object> tokenResponses = new ArrayDeque<>();
		private final Queue<Object> priceResponses = new ArrayDeque<>();
		private final List<String> requests = new ArrayList<>();
		private final List<String> usedTokens = new ArrayList<>();
		private int issuedTokens;

		private FakeKraken() {
			this.fetcher.addEndpointHandler(this::handle);
		}

		private HttpResponse<String> handle(Endpoint endpoint) throws HttpError {
			if (endpoint.body().contains("obtainKrakenToken")) {
				this.requests.add("token");
				var next = this.tokenResponses.poll();
				return next == null ? HttpResponse.ok(Data.token("token-" + ++this.issuedTokens)) : answer(next);
			}
			this.requests.add("prices");
			this.usedTokens.add(endpoint.properties().get("Authorization"));
			var next = this.priceResponses.poll();
			return next == null ? HttpResponse.ok(VALID_FORECAST) : answer(next);
		}

		private static HttpResponse<String> answer(Object next) throws HttpError {
			if (next instanceof HttpError error) {
				throw error;
			}
			if (next instanceof HttpStatus status) {
				return new HttpResponse<>(status, "{}");
			}
			return HttpResponse.ok((String) next);
		}

		private TariffOctopusDynamicGridBuyImpl activate(MyConfig.Builder config) throws Exception {
			var sut = new TariffOctopusDynamicGridBuyImpl();
			new ComponentTest(sut) //
					.addReference("meta", new DummyMeta().withCurrency(EUR)) //
					.addReference("componentManager", new DummyComponentManager(this.clock)) //
					.addReference("httpBridgeFactory",
							DummyBridgeHttpFactory.ofBridgeImpl(() -> this.fetcher, () -> this.executor)) //
					.activate(config.build());
			return sut;
		}

		private TariffOctopusDynamicGridBuyImpl activate() throws Exception {
			return this.activate(validConfig());
		}

		private void runDueRequests() {
			this.executor.update();
		}

		private void leapAndRun(long minutes) {
			this.clock.leap(minutes, MINUTES);
			this.executor.update();
		}
	}

	private static MyConfig.Builder validConfig() {
		return MyConfig.create() //
				.setId("timeOfUseTariff0") //
				.setEmail("octopus@example.test") //
				.setPassword("test-password") //
				.setAccountNumber("A-1234ABCD");
	}

	private static Object channel(TariffOctopusDynamicGridBuyImpl sut, TariffOctopusDynamicGridBuy.ChannelId id) {
		return sut.channel(id).getNextValue().get();
	}

	@Test
	void fetchesTokenAndPricesAndReusesToken() throws Exception {
		var kraken = new FakeKraken();
		final var sut = kraken.activate();
		kraken.runDueRequests();

		assertEquals(List.of("token", "prices"), kraken.requests);
		assertEquals("token-1", kraken.usedTokens.get(0));
		assertArrayEquals(new Double[] { 215.0, 182.5, 200.0, 220.0 }, sut.getPrices().asArray());
		assertEquals(200, channel(sut, TariffOctopusDynamicGridBuy.ChannelId.HTTP_STATUS_CODE));
		assertEquals(Instant.parse("2026-10-11T11:00:00Z").getEpochSecond(),
				channel(sut, TariffOctopusDynamicGridBuy.ChannelId.FORECAST_VALID_UNTIL));
		assertEquals(false, channel(sut, TariffOctopusDynamicGridBuy.ChannelId.STATUS_UPDATE_FAILED));

		// Next refresh after the configured interval, without new login
		kraken.leapAndRun(29);
		assertEquals(2, kraken.requests.size());
		kraken.leapAndRun(2);
		assertEquals(List.of("token", "prices", "prices"), kraken.requests);
	}

	@Test
	void reauthenticatesOnceAfterUnauthorizedPriceRequest() throws Exception {
		var kraken = new FakeKraken();
		kraken.priceResponses.add(HttpStatus.UNAUTHORIZED);
		final var sut = kraken.activate();
		kraken.runDueRequests();

		assertEquals(List.of("token", "prices", "token", "prices"), kraken.requests);
		assertEquals(List.of("token-1", "token-2"), kraken.usedTokens);
		assertFalse(sut.getPrices().isEmpty());
		assertEquals(false, channel(sut, TariffOctopusDynamicGridBuy.ChannelId.STATUS_AUTHENTICATION_FAILED));
	}

	@Test
	void reauthenticatesOnceAfterExpiredTokenGraphQlError() throws Exception {
		var kraken = new FakeKraken();
		kraken.priceResponses.add(Data.GRAPHQL_AUTHORIZATION_ERROR);
		final var sut = kraken.activate();
		kraken.runDueRequests();

		assertEquals(List.of("token", "prices", "token", "prices"), kraken.requests);
		assertFalse(sut.getPrices().isEmpty());
	}

	@Test
	void stopsAfterUnauthorized() throws Exception {
		var kraken = new FakeKraken();
		kraken.priceResponses.add(HttpStatus.UNAUTHORIZED);
		kraken.priceResponses.add(HttpStatus.UNAUTHORIZED);
		final var sut = kraken.activate();
		kraken.runDueRequests();

		assertEquals(4, kraken.requests.size());
		assertEquals(401, channel(sut, TariffOctopusDynamicGridBuy.ChannelId.HTTP_STATUS_CODE));
		assertEquals(true, channel(sut, TariffOctopusDynamicGridBuy.ChannelId.STATUS_AUTHENTICATION_FAILED));

		// No further requests until the configuration is changed
		kraken.leapAndRun(24 * 60);
		assertEquals(4, kraken.requests.size());
	}

	@Test
	void stopsAfterRejectedLogin() throws Exception {
		var kraken = new FakeKraken();
		kraken.tokenResponses.add(Data.GRAPHQL_AUTHORIZATION_ERROR);
		final var sut = kraken.activate();
		kraken.runDueRequests();

		assertEquals(List.of("token"), kraken.requests);
		assertEquals(true, channel(sut, TariffOctopusDynamicGridBuy.ChannelId.STATUS_AUTHENTICATION_FAILED));

		kraken.leapAndRun(24 * 60);
		assertEquals(1, kraken.requests.size());
	}

	@Test
	void retriesLateAfterRateLimit() throws Exception {
		var kraken = new FakeKraken();
		kraken.priceResponses.add(HttpStatus.TOO_MANY_REQUESTS);
		final var sut = kraken.activate();
		kraken.runDueRequests();

		assertEquals(2, kraken.requests.size());
		assertEquals(429, channel(sut, TariffOctopusDynamicGridBuy.ChannelId.HTTP_STATUS_CODE));
		assertEquals(true, channel(sut, TariffOctopusDynamicGridBuy.ChannelId.STATUS_RATE_LIMITED));
		assertEquals(false, channel(sut, TariffOctopusDynamicGridBuy.ChannelId.STATUS_AUTHENTICATION_FAILED));

		kraken.leapAndRun(59);
		assertEquals(2, kraken.requests.size());

		// Recovery at 11:06 with a forecast covering the current quarter
		kraken.priceResponses.add(Data.forecast(Data.agreement(//
				Data.slot("2026-10-11T11:00:00Z", "2026-10-11T11:15:00Z", "19"), //
				Data.slot("2026-10-11T11:15:00Z", "2026-10-11T11:30:00Z", "17"))));
		kraken.leapAndRun(2);
		assertEquals(3, kraken.requests.size());
		assertEquals(false, channel(sut, TariffOctopusDynamicGridBuy.ChannelId.STATUS_RATE_LIMITED));
		assertEquals(false, channel(sut, TariffOctopusDynamicGridBuy.ChannelId.STATUS_UPDATE_FAILED));
		assertArrayEquals(new Double[] { 190.0, 170.0 }, sut.getPrices().asArray());
	}

	@Test
	void retriesLateAfterGraphQlRateLimit() throws Exception {
		var kraken = new FakeKraken();
		kraken.priceResponses.add(Data.GRAPHQL_RATE_LIMIT_ERROR);
		final var sut = kraken.activate();
		kraken.runDueRequests();

		// KT-CT-1199 is no session expiry: no new login, no stop
		assertEquals(List.of("token", "prices"), kraken.requests);
		assertEquals(true, channel(sut, TariffOctopusDynamicGridBuy.ChannelId.STATUS_RATE_LIMITED));
		assertEquals(false, channel(sut, TariffOctopusDynamicGridBuy.ChannelId.STATUS_AUTHENTICATION_FAILED));

		kraken.leapAndRun(59);
		assertEquals(2, kraken.requests.size());
		kraken.leapAndRun(2);
		assertEquals(3, kraken.requests.size());
	}

	@Test
	void retriesShortlyAfterBadRequestOnLogin() throws Exception {
		var kraken = new FakeKraken();
		kraken.tokenResponses.add(HttpStatus.BAD_REQUEST);
		final var sut = kraken.activate();
		kraken.runDueRequests();

		assertEquals(List.of("token"), kraken.requests);
		assertEquals(400, channel(sut, TariffOctopusDynamicGridBuy.ChannelId.HTTP_STATUS_CODE));
		assertEquals(true, channel(sut, TariffOctopusDynamicGridBuy.ChannelId.STATUS_UPDATE_FAILED));
		assertEquals(false, channel(sut, TariffOctopusDynamicGridBuy.ChannelId.STATUS_AUTHENTICATION_FAILED));

		kraken.leapAndRun(6);
		assertEquals(List.of("token", "token", "prices"), kraken.requests);
		assertFalse(sut.getPrices().isEmpty());
	}

	@Test
	void stopsAfterUnauthorizedLogin() throws Exception {
		var kraken = new FakeKraken();
		kraken.tokenResponses.add(HttpStatus.UNAUTHORIZED);
		final var sut = kraken.activate();
		kraken.runDueRequests();

		assertEquals(List.of("token"), kraken.requests);
		assertEquals(true, channel(sut, TariffOctopusDynamicGridBuy.ChannelId.STATUS_AUTHENTICATION_FAILED));
		kraken.leapAndRun(24 * 60);
		assertEquals(1, kraken.requests.size());
	}

	@Test
	void retriesShortlyAfterOtherErrorsAndKeepsPrices() throws Exception {
		var kraken = new FakeKraken();
		final var sut = kraken.activate();
		kraken.runDueRequests();
		assertFalse(sut.getPrices().isEmpty());

		// Server error on the next refresh
		kraken.priceResponses.add(HttpStatus.INTERNAL_SERVER_ERROR);
		kraken.leapAndRun(31);
		assertEquals(3, kraken.requests.size());
		assertEquals(500, channel(sut, TariffOctopusDynamicGridBuy.ChannelId.HTTP_STATUS_CODE));
		assertEquals(true, channel(sut, TariffOctopusDynamicGridBuy.ChannelId.STATUS_UPDATE_FAILED));
		assertArrayEquals(new Double[] { 200.0, 220.0 }, sut.getPrices().asArray());

		// Network error: short retry again
		kraken.priceResponses.add(new HttpError.UnknownError(new IOException("Simulated connection failure")));
		kraken.leapAndRun(6);
		assertEquals(4, kraken.requests.size());
		assertEquals(-1, channel(sut, TariffOctopusDynamicGridBuy.ChannelId.HTTP_STATUS_CODE));

		// GraphQL error that is not an authorization error: no new login
		kraken.priceResponses.add(Data.GRAPHQL_SERVER_ERROR);
		kraken.leapAndRun(6);
		assertEquals(5, kraken.requests.size());
		assertEquals(true, channel(sut, TariffOctopusDynamicGridBuy.ChannelId.STATUS_UPDATE_FAILED));

		// Invalid forecast does not overwrite the previous prices either
		kraken.priceResponses.add(Data.forecast(Data.agreement()));
		kraken.leapAndRun(6);
		assertEquals(6, kraken.requests.size());
		assertEquals(true, channel(sut, TariffOctopusDynamicGridBuy.ChannelId.STATUS_UPDATE_FAILED));
		assertArrayEquals(new Double[] { 220.0 }, sut.getPrices().asArray());

		// Recovers on the next successful refresh
		kraken.priceResponses.add(Data.forecast(Data.agreement(//
				Data.slot("2026-10-11T11:00:00Z", "2026-10-11T11:15:00Z", "19"))));
		kraken.leapAndRun(6);
		assertEquals(false, channel(sut, TariffOctopusDynamicGridBuy.ChannelId.STATUS_UPDATE_FAILED));
		assertArrayEquals(new Double[] { 190.0 }, sut.getPrices().asArray());
		assertEquals(List.of("token", "prices", "prices", "prices", "prices", "prices", "prices"), kraken.requests);
	}

	@Test
	void sendsNoRequestsForUnsupportedCountry() throws Exception {
		var kraken = new FakeKraken();
		final var sut = kraken.activate(validConfig().setCountry(Country.UNITED_KINGDOM));
		kraken.leapAndRun(60);

		assertTrue(kraken.requests.isEmpty());
		assertEquals(true, channel(sut, TariffOctopusDynamicGridBuy.ChannelId.STATUS_COUNTRY_NOT_SUPPORTED));
		assertTrue(sut.getPrices().isEmpty());
	}

	@Test
	void sendsNoRequestsWithIncompleteConfiguration() throws Exception {
		var kraken = new FakeKraken();
		final var sut = kraken.activate(validConfig().setPassword(""));
		kraken.leapAndRun(60);

		assertTrue(kraken.requests.isEmpty());
		assertEquals(true, channel(sut, TariffOctopusDynamicGridBuy.ChannelId.STATUS_CONFIGURATION_INCOMPLETE));
		assertEquals(false, channel(sut, TariffOctopusDynamicGridBuy.ChannelId.STATUS_COUNTRY_NOT_SUPPORTED));
	}

	@Test
	void disabledComponentSendsNoRequests() throws Exception {
		var kraken = new FakeKraken();
		kraken.activate(validConfig().setEnabled(false));
		kraken.leapAndRun(60);

		assertTrue(kraken.requests.isEmpty());
	}
}
