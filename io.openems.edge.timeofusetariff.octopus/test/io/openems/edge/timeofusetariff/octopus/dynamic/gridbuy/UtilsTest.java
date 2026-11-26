package io.openems.edge.timeofusetariff.octopus.dynamic.gridbuy;

import static io.openems.edge.timeofusetariff.octopus.dynamic.gridbuy.Utils.parsePrices;
import static io.openems.edge.timeofusetariff.octopus.dynamic.gridbuy.Utils.parseToken;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.timeofusetariff.octopus.dynamic.gridbuy.Utils.KrakenApiException;
import io.openems.edge.timeofusetariff.octopus.dynamic.gridbuy.Utils.UpdateResult;

/**
 * Offline tests for the Kraken response parsing. No network access or real
 * account data is used.
 */
class UtilsTest {

	private static final Instant NOW = Instant.parse("2026-10-11T10:05:00Z");

	@Test
	void convertsGrossCentPerKwhToCurrencyPerMwh() throws Exception {
		// Kraken returns decimals as JSON strings
		var prices = parsePrices(Data.forecast(Data.agreement(//
				Data.slot("2026-10-11T10:00:00Z", "2026-10-11T10:15:00Z", "\"21.5\""), //
				Data.slot("2026-10-11T10:15:00Z", "2026-10-11T10:30:00Z", "18.25"))), NOW);

		assertArrayEquals(new Double[] { 215.0, 182.5 }, prices.asArray());
		assertEquals(Instant.parse("2026-10-11T10:00:00Z"), prices.getFirstTime());
	}

	@Test
	void handlesOffsetTimestampsAcrossDaylightSavingTime() throws Exception {
		// 2026-10-25 03:00 CEST -> 02:00 CET; both 02:45+02:00 and 02:00+01:00 exist
		var prices = parsePrices(Data.forecast(Data.agreement(//
				Data.slot("2026-10-25T02:45:00+02:00", "2026-10-25T03:00:00+02:00", "10"), //
				Data.slot("2026-10-25T02:00:00+01:00", "2026-10-25T02:15:00+01:00", "11"), //
				Data.slot("2026-10-25T01:15:00Z", "2026-10-25T01:30:00Z", "12"))), //
				Instant.parse("2026-10-25T00:45:00Z"));

		assertArrayEquals(new Double[] { 100.0, 110.0, 120.0 }, prices.asArray());
	}

	@Test
	void skipsEmptyForecasts() throws Exception {
		var prices = parsePrices(Data.forecast(Data.agreement(), //
				Data.agreement(Data.slot("2026-10-11T10:00:00Z", "2026-10-11T10:15:00Z", "21.5"))), NOW);

		assertArrayEquals(new Double[] { 215.0 }, prices.asArray());
	}

	@Test
	void prefersActiveAgreementOnOverlap() throws Exception {
		// Old contract still delivers future prices that overlap the new contract
		var oldContract = Data.agreement(//
				Data.slot("2026-10-11T09:45:00Z", "2026-10-11T10:00:00Z", "30"), //
				Data.slot("2026-10-11T10:00:00Z", "2026-10-11T10:15:00Z", "30"), //
				Data.slot("2026-10-11T10:15:00Z", "2026-10-11T10:30:00Z", "30"));
		var newContract = Data.agreement(//
				Data.slot("2026-10-11T10:15:00Z", "2026-10-11T10:30:00Z", "20"), //
				Data.slot("2026-10-11T10:30:00Z", "2026-10-11T10:45:00Z", "20"));
		var activeNewContract = Data.agreement(//
				Data.slot("2026-10-11T10:00:00Z", "2026-10-11T10:15:00Z", "20"), //
				Data.slot("2026-10-11T10:15:00Z", "2026-10-11T10:30:00Z", "20"));

		// Old contract active: continues with the new contract after its end
		assertArrayEquals(new Double[] { 300.0, 300.0, 200.0 },
				parsePrices(Data.forecast(newContract, oldContract), NOW).asArray());

		// New contract active: its prices win over the overlapping old contract
		assertArrayEquals(new Double[] { 200.0, 200.0 },
				parsePrices(Data.forecast(oldContract, activeNewContract), NOW).asArray());
	}

	@Test
	void keepsContinuousPartOnGapBetweenAgreements() throws Exception {
		var first = Data.agreement(//
				Data.slot("2026-10-11T10:00:00Z", "2026-10-11T10:15:00Z", "21.5"), //
				Data.slot("2026-10-11T10:15:00Z", "2026-10-11T10:30:00Z", "22"));
		var afterGap = Data.agreement(//
				Data.slot("2026-10-11T11:00:00Z", "2026-10-11T11:15:00Z", "25"));

		assertArrayEquals(new Double[] { 215.0, 220.0 },
				parsePrices(Data.forecast(first, afterGap), NOW).asArray());
	}

	@Test
	void skipsInvalidAndConflictingSlots() throws Exception {
		var prices = parsePrices(Data.forecast(Data.agreement(//
				Data.slot("2026-10-11T10:00:00Z", "2026-10-11T10:15:00Z", "21.5"), //
				Data.slot("2026-10-11T10:07:00Z", "2026-10-11T10:22:00Z", "99"), // unaligned
				Data.slot("2026-10-11T10:15:00Z", "2026-10-11T10:30:00Z", "18"), //
				Data.slot("2026-10-11T10:15:00Z", "2026-10-11T10:30:00Z", "19"), // conflicting
				Data.slot("2026-10-11T10:30:00Z", "2026-10-11T10:45:00Z", "\"abc\""))), NOW);

		assertArrayEquals(new Double[] { 215.0 }, prices.asArray());
	}

	@Test
	void dropsElapsedPrices() throws Exception {
		var prices = parsePrices(Data.forecast(Data.agreement(//
				Data.slot("2026-10-11T09:45:00Z", "2026-10-11T10:00:00Z", "30"), //
				Data.slot("2026-10-11T10:00:00Z", "2026-10-11T10:15:00Z", "21.5"))), NOW);

		assertArrayEquals(new Double[] { 215.0 }, prices.asArray());
	}

	@Test
	void rejectsResponsesWithoutUsablePrices() {
		assertThrows(OpenemsNamedException.class, () -> parsePrices(Data.forecast(Data.agreement()), NOW));
		assertThrows(OpenemsNamedException.class, () -> parsePrices("{\"data\":{\"account\":null}}", NOW));
		assertThrows(OpenemsNamedException.class, () -> parsePrices("", NOW));
		assertThrows(OpenemsNamedException.class, () -> parsePrices(Data.forecast(Data.agreement(//
				Data.slot("2026-10-11T09:00:00Z", "2026-10-11T09:15:00Z", "30"))), NOW));
	}

	@Test
	void classifiesGraphQlErrors() {
		assertEquals(UpdateResult.AUTHENTICATION_FAILED,
				assertThrows(KrakenApiException.class, () -> parsePrices(Data.GRAPHQL_AUTHORIZATION_ERROR, NOW))
						.getResult());
		assertEquals(UpdateResult.RATE_LIMITED,
				assertThrows(KrakenApiException.class, () -> parsePrices(Data.GRAPHQL_RATE_LIMIT_ERROR, NOW))
						.getResult());
		assertEquals(UpdateResult.FAILED,
				assertThrows(KrakenApiException.class, () -> parsePrices(Data.GRAPHQL_SERVER_ERROR, NOW))
						.getResult());
		assertTrue(Utils.hasGraphQlAuthorizationError(Data.GRAPHQL_AUTHORIZATION_ERROR));
		assertFalse(Utils.hasGraphQlAuthorizationError(Data.GRAPHQL_RATE_LIMIT_ERROR));
		assertFalse(Utils.hasGraphQlAuthorizationError(Data.GRAPHQL_SERVER_ERROR));
	}

	@Test
	void parsesToken() throws Exception {
		assertEquals("test-token", parseToken(Data.token("test-token")));
		// GraphQL errors on login mean rejected credentials, except rate limits
		assertEquals(UpdateResult.AUTHENTICATION_FAILED,
				assertThrows(KrakenApiException.class, () -> parseToken(Data.GRAPHQL_SERVER_ERROR)).getResult());
		assertEquals(UpdateResult.RATE_LIMITED,
				assertThrows(KrakenApiException.class, () -> parseToken(Data.GRAPHQL_RATE_LIMIT_ERROR)).getResult());
		assertEquals(UpdateResult.FAILED, assertThrows(KrakenApiException.class,
				() -> parseToken("{\"data\":{\"obtainKrakenToken\":{\"token\":\"\"}}}")).getResult());
	}

	@Test
	void rejectsForecastStartingInTheFuture() {
		// Current quarter 10:00 is missing; 10:15 must not be presented as current
		assertThrows(OpenemsNamedException.class, () -> parsePrices(Data.forecast(Data.agreement(//
				Data.slot("2026-10-11T10:15:00Z", "2026-10-11T10:30:00Z", "21.5"))), NOW));
	}

	@Test
	void endpointsOfAllCountriesArePrepared() {
		for (var country : Country.values()) {
			assertTrue(country.getApiUrl().startsWith("https://"));
			assertTrue(country.getApiUrl().endsWith("/v1/graphql/"));
		}
		assertTrue(Country.GERMANY.isSupported());
		assertEquals(1, Arrays.stream(Country.values()).filter(Country::isSupported).count());
	}
}
