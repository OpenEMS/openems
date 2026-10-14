package io.openems.edge.timeofusetariff.octopusdynamic;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;

import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Offline contract tests for Octopus DE quarter-hour prices. No real account
 * data, network access or credentials are required.
 */
public class OctopusDynamicDeImplTest {

	@Test
	public void rateLimitHonorsRetryAfter() {
		assertEquals(true, OctopusDynamicDeImpl.retryDelaySeconds(429, 1, 3600) >= 3600);
	}

	@Test
	public void exponentialBackoffIncreases() {
		assertEquals(true, OctopusDynamicDeImpl.retryDelaySeconds(503, 2, 0)
				> OctopusDynamicDeImpl.retryDelaySeconds(503, 1, 0));
	}

	@Test
	public void parsesRetryAfterHeader() {
		assertEquals(120L, OctopusDynamicDeImpl.parseRetryAfter("120"));
		assertEquals(0L, OctopusDynamicDeImpl.parseRetryAfter("invalid"));
	}

	@Test
	public void reauthenticatesOnceAfterUnauthorizedForecast() {
		var provider = new StubProvider();
		provider.respond("{\"obtainKrakenToken\":{\"token\":\"first-token\"}}");
		provider.failWithHttpStatus(401);
		provider.respond("{\"obtainKrakenToken\":{\"token\":\"renewed-token\"}}");
		provider.respond(validFutureForecast());
		assertEquals(true, provider.refresh("A-TEST", "octopus@example.test", "test-password"));
		assertEquals(4, provider.requests);
		assertEquals("renewed-token", provider.suppliedToken);
	}

	@Test
	public void retainsPricesAfterRateLimit() {
		var provider = new StubProvider();
		provider.respond("{\"obtainKrakenToken\":{\"token\":\"mock-token\"}}");
		provider.respond(validFutureForecast());
		assertEquals(true, provider.refresh("A-TEST", "octopus@example.test", "test-password"));
		provider.failWithHttpStatus(429);
		assertEquals(false, provider.refresh("A-TEST", "octopus@example.test", "test-password"));
		assertArrayEquals(new Double[]{215.0}, provider.getPrices().asArray());
	}

	@Test
	public void invalidatesCachedTokenOnPasswordChange() {
		var provider = new StubProvider();
		provider.respond("{\"obtainKrakenToken\":{\"token\":\"old-token\"}}");
		provider.respond(validFutureForecast());
		provider.respond("{\"obtainKrakenToken\":{\"token\":\"new-token\"}}");
		provider.respond(validFutureForecast());
		assertEquals(true, provider.refresh("A-TEST", "octopus@example.test", "old-password"));
		assertEquals(true, provider.refresh("A-TEST", "octopus@example.test", "new-password"));
		assertEquals(4, provider.requests);
	}

	@Test
	public void diagnosticChannelsResetAfterSuccessfulRetry() {
		var provider = new StubProvider();
		provider.respond("{\"obtainKrakenToken\":{\"token\":\"mock-token\"}}");
		provider.failWithHttpStatus(429);
		assertEquals(false, provider.refresh("A-TEST", "octopus@example.test", "test-password"));
		assertEquals(true, provider.rateLimited);
		assertEquals(true, provider.nextRetry > 0);
		provider.respond(validFutureForecast());
		assertEquals(true, provider.refresh("A-TEST", "octopus@example.test", "test-password"));
		assertEquals(false, provider.rateLimited);
		assertEquals(false, provider.authenticationFailed);
		assertEquals(false, provider.serverError);
		assertEquals(false, provider.connectionFailed);
		assertEquals(java.time.Instant.parse("2099-01-01T12:15:00Z").getEpochSecond(),
				provider.forecastValidUntil);
	}

	@Test
	public void diagnosticChannelsDistinguishAuthAndServerFailures() {
		var auth = new StubProvider();
		auth.failWithHttpStatus(401);
		assertEquals(false, auth.refresh("A-TEST", "octopus@example.test", "test-password"));
		assertEquals(true, auth.authenticationFailed);
		assertEquals(false, auth.serverError);

		var server = new StubProvider();
		server.failWithHttpStatus(503);
		assertEquals(false, server.refresh("A-TEST", "octopus@example.test", "test-password"));
		assertEquals(true, server.serverError);
		assertEquals(false, server.authenticationFailed);
	}

	@Test
	public void retryAfterAndBackoffBoundaryCases() {
		assertEquals(0L, OctopusDynamicDeImpl.parseRetryAfter(null));
		assertEquals(0L, OctopusDynamicDeImpl.parseRetryAfter("-10"));
		assertEquals(true, OctopusDynamicDeImpl.retryDelaySeconds(429, 1, 7200) >= 7200);
		assertEquals(true, OctopusDynamicDeImpl.retryDelaySeconds(503, 10, 0) <= 2520);
		assertEquals(true, OctopusDynamicDeImpl.retryDelaySeconds(401, 1, 0) >= 43200);
	}

	private static String forecast(String slots) {
		return """
				{"account":{"properties":[{"electricityMalos":[{"agreements":[{
				  "unitRateForecast":[%s]
				}]}]}]}}
				""".formatted(slots);
	}

	private static String slot(String from, String to, String price) {
		return """
				{"validFrom":"%s","validTo":"%s",
				 "unitRateInformation":{"rates":[{"latestGrossUnitRateCentsPerKwh":%s}]}}
				""".formatted(from, to, price);
	}

	private static io.openems.edge.timeofusetariff.api.TimeOfUsePrices parse(String json) throws IOException {
		return OctopusDynamicDeImpl.parsePrices(JsonParser.parseString(json).getAsJsonObject());
	}


	private static final class StubProvider extends OctopusDynamicDeImpl {

		private final Queue<Object> responses = new ArrayDeque<>();
		private int requests;
		private String suppliedToken;
		private boolean authenticationFailed;
		private boolean rateLimited;
		private boolean connectionFailed;
		private boolean serverError;
		private long forecastValidUntil;
		private long nextRetry;

		@Override
		public void _setStatusAuthenticationFailed(boolean value) {
			this.authenticationFailed = value;
		}

		@Override
		public void _setStatusRateLimited(boolean value) {
			this.rateLimited = value;
		}

		@Override
		public void _setStatusConnectionFailed(boolean value) {
			this.connectionFailed = value;
		}

		@Override
		public void _setStatusServerError(boolean value) {
			this.serverError = value;
		}

		@Override
		public void _setForecastValidUntil(long value) {
			this.forecastValidUntil = value;
		}

		@Override
		public void _setNextRetry(long value) {
			this.nextRetry = value;
		}

		void respond(String json) {
			this.responses.add(JsonParser.parseString(json).getAsJsonObject());
		}

		void failWithHttpStatus(int status) {
			this.responses.add(new OctopusDynamicDeImpl.HttpFailure(status, 0));
		}

		void failWithIoException() {
			this.responses.add(new IOException("Simulated connection failure"));
		}

		@Override
		JsonObject post(String query, JsonObject variables, String token) throws IOException {
			this.requests++;
			this.suppliedToken = token;
			var next = this.responses.remove();
			if (next instanceof IOException exception) {
				throw exception;
			}
			return (JsonObject) next;
		}
	}

	private static String validFutureForecast() {
		return forecast(slot("2099-01-01T12:00:00Z", "2099-01-01T12:15:00Z", "21.5"));
	}

	@Test
	public void authenticatesAndUsesTokenForAccountForecast() {
		var provider = new StubProvider();
		provider.respond("{\"obtainKrakenToken\":{\"token\":\"mock-token\"}}");
		provider.respond(validFutureForecast());

		assertEquals(true, provider.refresh("A-TEST", "octopus@example.test", "test-password"));
		assertEquals(2, provider.requests);
		assertEquals("mock-token", provider.suppliedToken);
		assertArrayEquals(new Double[]{215.0}, provider.getPrices().asArray());
	}

	@Test
	public void rejectsMissingCredentialsWithoutNetworkRequests() {
		var provider = new StubProvider();

		assertEquals(false, provider.refresh("A-TEST", "", "test-password"));
		assertEquals(false, provider.refresh("A-TEST", "octopus@example.test", ""));
		assertEquals(false, provider.refresh("", "octopus@example.test", "test-password"));
		assertEquals(0, provider.requests);
	}

	@Test
	public void rejectsMissingAuthenticationToken() {
		var provider = new StubProvider();
		provider.respond("{\"obtainKrakenToken\":{}}");

		assertEquals(false, provider.refresh("A-TEST", "octopus@example.test", "test-password"));
		assertEquals(1, provider.requests);
	}

	@Test
	public void retainsValidatedForecastWhenConnectionFails() {
		var provider = new StubProvider();
		provider.respond("{\"obtainKrakenToken\":{\"token\":\"mock-token\"}}");
		provider.respond(validFutureForecast());
		assertEquals(true, provider.refresh("A-TEST", "octopus@example.test", "test-password"));

		provider.failWithIoException();
		assertEquals(false, provider.refresh("A-TEST", "octopus@example.test", "test-password"));
		assertArrayEquals(new Double[]{215.0}, provider.getPrices().asArray());
	}

	

	@Test
	public void rejectsInvalidEmailOrPasswordResponse() {
		var provider = new StubProvider();
		provider.failWithIoException();

		assertEquals(false, provider.refresh("A-TEST", "invalid@example.test", "wrong-password"));
		assertEquals(1, provider.requests);
	}

	@Test
	public void cachesTokenBetweenSuccessfulForecastRequests() {
		var provider = new StubProvider();
		provider.respond("{\"obtainKrakenToken\":{\"token\":\"mock-token\"}}");
		provider.respond(validFutureForecast());
		provider.respond(validFutureForecast());
		assertEquals(true, provider.refresh("A-TEST", "octopus@example.test", "test-password"));
		assertEquals(true, provider.refresh("A-TEST", "octopus@example.test", "test-password"));
		assertEquals(3, provider.requests);
	}

	@Test
	public void rejectsUnknownAccountWithoutOverwritingValidPrices() {
		var provider = new StubProvider();
		provider.respond("{\"obtainKrakenToken\":{\"token\":\"mock-token\"}}");
		provider.respond(validFutureForecast());
		assertEquals(true, provider.refresh("A-TEST", "octopus@example.test", "test-password"));

		provider.respond("{\"account\":null}");

		assertEquals(false, provider.refresh("A-UNKNOWN", "octopus@example.test", "test-password"));
		assertArrayEquals(new Double[]{215.0}, provider.getPrices().asArray());
	}

	@Test
	public void rejectsNetworkFailureDuringPriceRequest() {
		var provider = new StubProvider();
		provider.respond("{\"obtainKrakenToken\":{\"token\":\"mock-token\"}}");
		provider.failWithIoException();

		assertEquals(false, provider.refresh("A-TEST", "octopus@example.test", "test-password"));
		assertEquals(2, provider.requests);
	}

	@Test
	public void classifiesAuthenticationAndAccountErrorsWithoutLoggingCredentials() {
		assertEquals("AUTHENTICATION_REJECTED",
				OctopusDynamicDeImpl.categorizeFailure("authentication", new IOException("HTTP_STATUS_401")));
		assertEquals("AUTHENTICATION_REJECTED",
				OctopusDynamicDeImpl.categorizeFailure("authentication", new IOException("GRAPHQL_ERRORS")));
		assertEquals("ACCOUNT_ACCESS_DENIED",
				OctopusDynamicDeImpl.categorizeFailure("forecast", new IOException("HTTP_STATUS_403")));
		assertEquals("ACCOUNT_NOT_FOUND",
				OctopusDynamicDeImpl.categorizeFailure("price-validation", new IOException("ACCOUNT_NOT_FOUND")));
		assertEquals("REQUEST_FAILED",
				OctopusDynamicDeImpl.categorizeFailure("authentication", new IOException("Sensitive provider response")));
		assertEquals("REQUEST_FAILED",
				OctopusDynamicDeImpl.categorizeFailure("authentication", new IOException()));
	}

	@Test
	public void refreshCadenceUsesNormalIntervalAndFiveMinuteRetry() {
		assertEquals(30L, OctopusDynamicDeImpl.nextRefreshDelayMinutes(true, 30));
		assertEquals(15L, OctopusDynamicDeImpl.nextRefreshDelayMinutes(true, 5));
		assertEquals(5L, OctopusDynamicDeImpl.nextRefreshDelayMinutes(false, 30));
		assertEquals(5L, OctopusDynamicDeImpl.nextRefreshDelayMinutes(false, 15));
	}

	@Test
	public void convertsGrossCentPerKwhToEurPerMwh() throws Exception {
		var first = slot("2026-10-11T10:00:00Z", "2026-10-11T10:15:00Z", "21.5");
		var second = slot("2026-10-11T10:15:00Z", "2026-10-11T10:30:00Z", "18.25");
		assertArrayEquals(new Double[]{215.0, 182.5}, parse(forecast(first + "," + second)).asArray());
	}

	@Test
	public void rejectsMissingQuarter() {
		var first = slot("2026-10-11T10:00:00Z", "2026-10-11T10:15:00Z", "21.5");
		var third = slot("2026-10-11T10:30:00Z", "2026-10-11T10:45:00Z", "18.25");
		assertThrows(IOException.class, () -> parse(forecast(first + "," + third)));
	}

	@Test
	public void rejectsInvalidSlotDuration() {
		var broken = slot("2026-10-11T10:00:00Z", "2026-10-11T11:00:00Z", "21.5");
		assertThrows(IOException.class, () -> parse(forecast(broken)));
	}

	@Test
	public void rejectsAmbiguousAgreements() {
		var a = slot("2026-10-11T10:00:00Z", "2026-10-11T10:15:00Z", "21.5");
		var b = slot("2026-10-11T10:00:00Z", "2026-10-11T10:15:00Z", "27.0");
		assertThrows(IOException.class, () -> parse(forecast(a + "," + b)));
	}

	@Test
	public void rejectsEmptyOrMalformedData() {
		assertThrows(IOException.class, () -> parse(forecast("")));
		assertThrows(IOException.class, () -> parse("{}"));
	}

	@Test
	public void handlesUtcTimestampsAcrossDaylightSavingTime() throws Exception {
		var a = slot("2026-10-25T00:45:00Z", "2026-10-25T01:00:00Z", "10");
		var b = slot("2026-10-25T01:00:00Z", "2026-10-25T01:15:00Z", "11");
		assertArrayEquals(new Double[]{100.0, 110.0}, parse(forecast(a + "," + b)).asArray());
	}

	@Test
	public void classifiesProviderConnectivityFailures() {
		assertEquals("NETWORK_TIMEOUT", OctopusDynamicDeImpl.categorizeFailure("forecast",
				new java.net.SocketTimeoutException()));
		assertEquals("API_RATE_LIMITED", OctopusDynamicDeImpl.categorizeFailure("forecast",
				new IOException("HTTP_STATUS_429")));
		assertEquals("INVALID_PRICE_FORECAST", OctopusDynamicDeImpl.categorizeFailure("price-validation",
				new IOException("Missing quarter-hour price")));
	}

}
