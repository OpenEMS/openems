package io.openems.edge.timeofusetariff.ews;

import static io.openems.edge.timeofusetariff.ews.TimeOfUseTariffEwsImpl.CLIENT_ERROR_CODE;
import static io.openems.edge.timeofusetariff.ews.Utils.calculateDelay;
import static io.openems.edge.timeofusetariff.ews.Utils.parsePrices;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

public class UtilsTest {

	private static final int SUCCESS_CODE = 200;
	private static final int SERVER_ERROR_CODE = 500;

	private static String JSON_DATA = """
			{
	"tariff": "EWS ÖKO DYN",
	"priceUnit": "ct/kWh",
	"interval": 15,
	"intervalUnit": "minutes",
	"today": [
		{
			"total": 32.13,
			"dynamic": 10.33,
			"fix": 21.80,
			"startsAt": "2025-09-01T00:00:00.000+02:00"
		},
		{
			"total": 32.02,
			"dynamic": 10.22,
			"fix": 21.80,
			"startsAt": "2025-09-01T00:15:00.000+02:00"
		},
		{
			"total": 31.68,
			"dynamic": 9.88,
			"fix": 21.80,
			"startsAt": "2025-09-01T00:30:00.000+02:00"
		},
		{
			"total": 30.98,
			"dynamic": 9.18,
			"fix": 21.80,
			"startsAt": "2025-09-01T00:45:00.000+02:00"
		},
		{
			"total": 31.97,
			"dynamic": 10.17,
			"fix": 21.80,
			"startsAt": "2025-09-01T01:00:00.000+02:00"
		},
		{
			"total": 31.79,
			"dynamic": 9.99,
			"fix": 21.80,
			"startsAt": "2025-09-01T01:15:00.000+02:00"
		},
		{
			"total": 31.74,
			"dynamic": 9.94,
			"fix": 21.80,
			"startsAt": "2025-09-01T01:30:00.000+02:00"
		},
		{
			"total": 31.23,
			"dynamic": 9.43,
			"fix": 21.80,
			"startsAt": "2025-09-01T01:45:00.000+02:00"
		},
		{
			"total": 31.45,
			"dynamic": 9.65,
			"fix": 21.80,
			"startsAt": "2025-09-01T02:00:00.000+02:00"
		},
		{
			"total": 31.44,
			"dynamic": 9.64,
			"fix": 21.80,
			"startsAt": "2025-09-01T02:15:00.000+02:00"
		},
		{
			"total": 31.69,
			"dynamic": 9.89,
			"fix": 21.80,
			"startsAt": "2025-09-01T02:30:00.000+02:00"
		},
		{
			"total": 31.57,
			"dynamic": 9.77,
			"fix": 21.80,
			"startsAt": "2025-09-01T02:45:00.000+02:00"
		},
		{
			"total": 31.38,
			"dynamic": 9.58,
			"fix": 21.80,
			"startsAt": "2025-09-01T03:00:00.000+02:00"
		},
		{
			"total": 31.32,
			"dynamic": 9.52,
			"fix": 21.80,
			"startsAt": "2025-09-01T03:15:00.000+02:00"
		},
		{
			"total": 31.44,
			"dynamic": 9.64,
			"fix": 21.80,
			"startsAt": "2025-09-01T03:30:00.000+02:00"
		},
		{
			"total": 31.57,
			"dynamic": 9.77,
			"fix": 21.80,
			"startsAt": "2025-09-01T03:45:00.000+02:00"
		},
		{
			"total": 30.75,
			"dynamic": 8.95,
			"fix": 21.80,
			"startsAt": "2025-09-01T04:00:00.000+02:00"
		},
		{
			"total": 30.94,
			"dynamic": 9.14,
			"fix": 21.80,
			"startsAt": "2025-09-01T04:15:00.000+02:00"
		},
		{
			"total": 32.00,
			"dynamic": 10.20,
			"fix": 21.80,
			"startsAt": "2025-09-01T04:30:00.000+02:00"
		},
		{
			"total": 32.11,
			"dynamic": 10.31,
			"fix": 21.80,
			"startsAt": "2025-09-01T04:45:00.000+02:00"
		},
		{
			"total": 31.23,
			"dynamic": 9.43,
			"fix": 21.80,
			"startsAt": "2025-09-01T05:00:00.000+02:00"
		},
		{
			"total": 31.56,
			"dynamic": 9.76,
			"fix": 21.80,
			"startsAt": "2025-09-01T05:15:00.000+02:00"
		},
		{
			"total": 32.89,
			"dynamic": 11.09,
			"fix": 21.80,
			"startsAt": "2025-09-01T05:30:00.000+02:00"
		},
		{
			"total": 34.17,
			"dynamic": 12.37,
			"fix": 21.80,
			"startsAt": "2025-09-01T05:45:00.000+02:00"
		},
		{
			"total": 31.89,
			"dynamic": 10.09,
			"fix": 21.80,
			"startsAt": "2025-09-01T06:00:00.000+02:00"
		},
		{
			"total": 35.14,
			"dynamic": 13.34,
			"fix": 21.80,
			"startsAt": "2025-09-01T06:15:00.000+02:00"
		},
		{
			"total": 36.09,
			"dynamic": 14.29,
			"fix": 21.80,
			"startsAt": "2025-09-01T06:30:00.000+02:00"
		},
		{
			"total": 36.91,
			"dynamic": 15.11,
			"fix": 21.80,
			"startsAt": "2025-09-01T06:45:00.000+02:00"
		},
		{
			"total": 37.14,
			"dynamic": 15.34,
			"fix": 21.80,
			"startsAt": "2025-09-01T07:00:00.000+02:00"
		},
		{
			"total": 36.73,
			"dynamic": 14.93,
			"fix": 21.80,
			"startsAt": "2025-09-01T07:15:00.000+02:00"
		},
		{
			"total": 35.98,
			"dynamic": 14.18,
			"fix": 21.80,
			"startsAt": "2025-09-01T07:30:00.000+02:00"
		},
		{
			"total": 35.26,
			"dynamic": 13.46,
			"fix": 21.80,
			"startsAt": "2025-09-01T07:45:00.000+02:00"
		},
		{
			"total": 37.00,
			"dynamic": 15.20,
			"fix": 21.80,
			"startsAt": "2025-09-01T08:00:00.000+02:00"
		},
		{
			"total": 35.74,
			"dynamic": 13.94,
			"fix": 21.80,
			"startsAt": "2025-09-01T08:15:00.000+02:00"
		},
		{
			"total": 34.28,
			"dynamic": 12.48,
			"fix": 21.80,
			"startsAt": "2025-09-01T08:30:00.000+02:00"
		},
		{
			"total": 32.49,
			"dynamic": 10.69,
			"fix": 21.80,
			"startsAt": "2025-09-01T08:45:00.000+02:00"
		},
		{
			"total": 34.71,
			"dynamic": 12.91,
			"fix": 21.80,
			"startsAt": "2025-09-01T09:00:00.000+02:00"
		},
		{
			"total": 33.23,
			"dynamic": 11.43,
			"fix": 21.80,
			"startsAt": "2025-09-01T09:15:00.000+02:00"
		},
		{
			"total": 32.29,
			"dynamic": 10.49,
			"fix": 21.80,
			"startsAt": "2025-09-01T09:30:00.000+02:00"
		},
		{
			"total": 31.10,
			"dynamic": 9.30,
			"fix": 21.80,
			"startsAt": "2025-09-01T09:45:00.000+02:00"
		},
		{
			"total": 33.11,
			"dynamic": 11.31,
			"fix": 21.80,
			"startsAt": "2025-09-01T10:00:00.000+02:00"
		},
		{
			"total": 31.53,
			"dynamic": 9.73,
			"fix": 21.80,
			"startsAt": "2025-09-01T10:15:00.000+02:00"
		},
		{
			"total": 30.37,
			"dynamic": 8.57,
			"fix": 21.80,
			"startsAt": "2025-09-01T10:30:00.000+02:00"
		},
		{
			"total": 28.83,
			"dynamic": 7.03,
			"fix": 21.80,
			"startsAt": "2025-09-01T10:45:00.000+02:00"
		},
		{
			"total": 31.24,
			"dynamic": 9.44,
			"fix": 21.80,
			"startsAt": "2025-09-01T11:00:00.000+02:00"
		},
		{
			"total": 30.42,
			"dynamic": 8.62,
			"fix": 21.80,
			"startsAt": "2025-09-01T11:15:00.000+02:00"
		},
		{
			"total": 30.01,
			"dynamic": 8.21,
			"fix": 21.80,
			"startsAt": "2025-09-01T11:30:00.000+02:00"
		},
		{
			"total": 28.41,
			"dynamic": 6.61,
			"fix": 21.80,
			"startsAt": "2025-09-01T11:45:00.000+02:00"
		},
		{
			"total": 28.73,
			"dynamic": 6.93,
			"fix": 21.80,
			"startsAt": "2025-09-01T12:00:00.000+02:00"
		},
		{
			"total": 26.99,
			"dynamic": 5.19,
			"fix": 21.80,
			"startsAt": "2025-09-01T12:15:00.000+02:00"
		},
		{
			"total": 26.29,
			"dynamic": 4.49,
			"fix": 21.80,
			"startsAt": "2025-09-01T12:30:00.000+02:00"
		},
		{
			"total": 25.66,
			"dynamic": 3.86,
			"fix": 21.80,
			"startsAt": "2025-09-01T12:45:00.000+02:00"
		},
		{
			"total": 26.07,
			"dynamic": 4.27,
			"fix": 21.80,
			"startsAt": "2025-09-01T13:00:00.000+02:00"
		},
		{
			"total": 25.60,
			"dynamic": 3.80,
			"fix": 21.80,
			"startsAt": "2025-09-01T13:15:00.000+02:00"
		},
		{
			"total": 25.18,
			"dynamic": 3.38,
			"fix": 21.80,
			"startsAt": "2025-09-01T13:30:00.000+02:00"
		},
		{
			"total": 24.20,
			"dynamic": 2.40,
			"fix": 21.80,
			"startsAt": "2025-09-01T13:45:00.000+02:00"
		},
		{
			"total": 25.44,
			"dynamic": 3.64,
			"fix": 21.80,
			"startsAt": "2025-09-01T14:00:00.000+02:00"
		},
		{
			"total": 26.29,
			"dynamic": 4.49,
			"fix": 21.80,
			"startsAt": "2025-09-01T14:15:00.000+02:00"
		},
		{
			"total": 27.71,
			"dynamic": 5.91,
			"fix": 21.80,
			"startsAt": "2025-09-01T14:30:00.000+02:00"
		},
		{
			"total": 29.03,
			"dynamic": 7.23,
			"fix": 21.80,
			"startsAt": "2025-09-01T14:45:00.000+02:00"
		},
		{
			"total": 27.23,
			"dynamic": 5.43,
			"fix": 21.80,
			"startsAt": "2025-09-01T15:00:00.000+02:00"
		},
		{
			"total": 29.52,
			"dynamic": 7.72,
			"fix": 21.80,
			"startsAt": "2025-09-01T15:15:00.000+02:00"
		},
		{
			"total": 30.51,
			"dynamic": 8.71,
			"fix": 21.80,
			"startsAt": "2025-09-01T15:30:00.000+02:00"
		},
		{
			"total": 31.07,
			"dynamic": 9.27,
			"fix": 21.80,
			"startsAt": "2025-09-01T15:45:00.000+02:00"
		},
		{
			"total": 30.50,
			"dynamic": 8.70,
			"fix": 21.80,
			"startsAt": "2025-09-01T16:00:00.000+02:00"
		},
		{
			"total": 30.83,
			"dynamic": 9.03,
			"fix": 21.80,
			"startsAt": "2025-09-01T16:15:00.000+02:00"
		},
		{
			"total": 32.91,
			"dynamic": 11.11,
			"fix": 21.80,
			"startsAt": "2025-09-01T16:30:00.000+02:00"
		},
		{
			"total": 34.69,
			"dynamic": 12.89,
			"fix": 21.80,
			"startsAt": "2025-09-01T16:45:00.000+02:00"
		},
		{
			"total": 31.22,
			"dynamic": 9.42,
			"fix": 21.80,
			"startsAt": "2025-09-01T17:00:00.000+02:00"
		},
		{
			"total": 33.07,
			"dynamic": 11.27,
			"fix": 21.80,
			"startsAt": "2025-09-01T17:15:00.000+02:00"
		},
		{
			"total": 36.05,
			"dynamic": 14.25,
			"fix": 21.80,
			"startsAt": "2025-09-01T17:30:00.000+02:00"
		},
		{
			"total": 38.36,
			"dynamic": 16.56,
			"fix": 21.80,
			"startsAt": "2025-09-01T17:45:00.000+02:00"
		},
		{
			"total": 35.44,
			"dynamic": 13.64,
			"fix": 21.80,
			"startsAt": "2025-09-01T18:00:00.000+02:00"
		},
		{
			"total": 37.62,
			"dynamic": 15.82,
			"fix": 21.80,
			"startsAt": "2025-09-01T18:15:00.000+02:00"
		},
		{
			"total": 41.56,
			"dynamic": 19.76,
			"fix": 21.80,
			"startsAt": "2025-09-01T18:30:00.000+02:00"
		},
		{
			"total": 46.42,
			"dynamic": 24.62,
			"fix": 21.80,
			"startsAt": "2025-09-01T18:45:00.000+02:00"
		},
		{
			"total": 45.47,
			"dynamic": 23.67,
			"fix": 21.80,
			"startsAt": "2025-09-01T19:00:00.000+02:00"
		},
		{
			"total": 49.09,
			"dynamic": 27.29,
			"fix": 21.80,
			"startsAt": "2025-09-01T19:15:00.000+02:00"
		},
		{
			"total": 51.99,
			"dynamic": 30.19,
			"fix": 21.80,
			"startsAt": "2025-09-01T19:30:00.000+02:00"
		},
		{
			"total": 54.59,
			"dynamic": 32.79,
			"fix": 21.80,
			"startsAt": "2025-09-01T19:45:00.000+02:00"
		},
		{
			"total": 51.61,
			"dynamic": 29.81,
			"fix": 21.80,
			"startsAt": "2025-09-01T20:00:00.000+02:00"
		},
		{
			"total": 46.86,
			"dynamic": 25.06,
			"fix": 21.80,
			"startsAt": "2025-09-01T20:15:00.000+02:00"
		},
		{
			"total": 43.68,
			"dynamic": 21.88,
			"fix": 21.80,
			"startsAt": "2025-09-01T20:30:00.000+02:00"
		},
		{
			"total": 38.67,
			"dynamic": 16.87,
			"fix": 21.80,
			"startsAt": "2025-09-01T20:45:00.000+02:00"
		},
		{
			"total": 38.50,
			"dynamic": 16.70,
			"fix": 21.80,
			"startsAt": "2025-09-01T21:00:00.000+02:00"
		},
		{
			"total": 36.95,
			"dynamic": 15.15,
			"fix": 21.80,
			"startsAt": "2025-09-01T21:15:00.000+02:00"
		},
		{
			"total": 35.90,
			"dynamic": 14.10,
			"fix": 21.80,
			"startsAt": "2025-09-01T21:30:00.000+02:00"
		},
		{
			"total": 34.41,
			"dynamic": 12.61,
			"fix": 21.80,
			"startsAt": "2025-09-01T21:45:00.000+02:00"
		},
		{
			"total": 37.19,
			"dynamic": 15.39,
			"fix": 21.80,
			"startsAt": "2025-09-01T22:00:00.000+02:00"
		},
		{
			"total": 35.47,
			"dynamic": 13.67,
			"fix": 21.80,
			"startsAt": "2025-09-01T22:15:00.000+02:00"
		},
		{
			"total": 34.61,
			"dynamic": 12.81,
			"fix": 21.80,
			"startsAt": "2025-09-01T22:30:00.000+02:00"
		},
		{
			"total": 33.09,
			"dynamic": 11.29,
			"fix": 21.80,
			"startsAt": "2025-09-01T22:45:00.000+02:00"
		},
		{
			"total": 35.52,
			"dynamic": 13.72,
			"fix": 21.80,
			"startsAt": "2025-09-01T23:00:00.000+02:00"
		},
		{
			"total": 34.38,
			"dynamic": 12.58,
			"fix": 21.80,
			"startsAt": "2025-09-01T23:15:00.000+02:00"
		},
		{
			"total": 33.12,
			"dynamic": 11.32,
			"fix": 21.80,
			"startsAt": "2025-09-01T23:30:00.000+02:00"
		},
		{
			"total": 32.37,
			"dynamic": 10.57,
			"fix": 21.80,
			"startsAt": "2025-09-01T23:45:00.000+02:00"
		}
	],
	"tomorrow": [
	]
}
			""";

	@Test
	public void nonEmptyStringTest() throws OpenemsNamedException {
		// Parsing with custom data
		var prices = parsePrices(JSON_DATA); //

		// To check if the Map is not empty
		assertFalse(prices.isEmpty());

		// To check if a value is present in map.
		assertEquals(32.13 * 10, prices.getFirst(), 0.001);
		
		// To check if a date is present in map.
		assertEquals(ZonedDateTime.of(2025, 9, 1, 0, 0, 0, 0, ZoneId.ofOffset("", ZoneOffset.ofHours(2))), prices.getFirstTime());

		// To check 15 minutes values are taken instead of one hour values.
		var firstHour = prices.getFirstTime();
		assertNotNull(prices.getAt(firstHour.plusMinutes(15)));
	}

	@Test(expected = OpenemsNamedException.class)
	public void emptyStringTest() throws OpenemsNamedException {
		// Parsing with empty string
		parsePrices("");
	}

	@Test
	public void testCalculateDelay() {
		// case: client error
		var httpStatusCode = CLIENT_ERROR_CODE;
		var unableToUpdatePrices = true;
		var delay = calculateDelay(httpStatusCode, unableToUpdatePrices);
		assertEquals(0, delay);

		// Case: server error
		httpStatusCode = SERVER_ERROR_CODE;
		unableToUpdatePrices = true;
		delay = calculateDelay(httpStatusCode, unableToUpdatePrices);
		var minDelay = 240; // 4 minutes and truncate to minutes is also considered.
		var maxDelay = 360; // 6 minutes
		assertTrue(delay >= minDelay && delay <= maxDelay);

		// case: no error.
		httpStatusCode = SUCCESS_CODE;
		unableToUpdatePrices = false;
		delay = calculateDelay(httpStatusCode, unableToUpdatePrices);
		var minSuccesDelay = 0; // 0 hours and truncate to hours is also considered.
		var maxSuccesDelay = 3660; // 1 hour
		assertTrue(delay >= minSuccesDelay && delay <= maxSuccesDelay);
	}

	@Test
	public void singleHomeParseTest() throws OpenemsNamedException {
		// Parsing with custom filter. Since the test is single home, the filter should
		// be ignored
		var prices = parsePrices(JSON_DATA); //

		// To check if the Map is not empty
		assertFalse(prices.isEmpty());

		// To check if a value is present in map.
		assertEquals(32.13 * 10, prices.getFirst(), 0.001);
	}
}
