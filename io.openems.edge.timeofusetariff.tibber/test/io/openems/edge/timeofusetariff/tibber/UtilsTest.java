package io.openems.edge.timeofusetariff.tibber;

import static io.openems.edge.timeofusetariff.tibber.Data.JSON_DATA;
import static io.openems.edge.timeofusetariff.tibber.TimeOfUseTariffTibberImpl.CLIENT_ERROR_CODE;
import static io.openems.edge.timeofusetariff.tibber.TimeOfUseTariffTibberImpl.TOO_MANY_REQUESTS_CODE;
import static io.openems.edge.timeofusetariff.tibber.Utils.calculateDelay;
import static io.openems.edge.timeofusetariff.tibber.Utils.generateGraphQl;
import static io.openems.edge.timeofusetariff.tibber.Utils.parsePrices;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

public class UtilsTest {

	private static final int SUCCESS_CODE = 200;
	private static final int SERVER_ERROR_CODE = 500;

	@Test
	public void nonEmptyStringTest() throws OpenemsNamedException {
		// Parsing with custom data
		var prices = parsePrices(JSON_DATA, null); //

		// To check if the Map is not empty
		assertFalse(prices.isEmpty());

		// To check if a value is present in map.
		assertEquals(0.2688 * 1000, prices.getFirst(), 0.001);

		// To check 15 minutes values are taken instead of one hour values.
		var firstHour = prices.getFirstTime();
		assertNotNull(prices.getAt(firstHour.plusMinutes(15)));
	}

	@Test(expected = OpenemsNamedException.class)
	public void emptyStringTest() throws OpenemsNamedException {
		// Parsing with empty string
		parsePrices("", null);
	}

	@Test
	public void generateGraphQlTest() {
		assertEquals("""
				{
				  viewer {
				    homes {
				      id
				      appNickname
				      currentSubscription{
				        priceInfo(resolution: QUARTER_HOURLY) {
				          today {
				            total
				            startsAt
				          }
				          tomorrow {
				            total
				            startsAt
				          }
				        }
				      }
				    }
				  }
				}""", generateGraphQl());
	}

	@Test
	public void testCalculateDelay() {
		// case: client error
		var httpStatusCode = CLIENT_ERROR_CODE;
		var unableToUpdatePrices = true;
		var filterIsRequired = false;
		var delay = calculateDelay(httpStatusCode, filterIsRequired, unableToUpdatePrices);
		assertEquals(0, delay);

		// case: filterIsRequired even though successful response.
		httpStatusCode = 200;
		filterIsRequired = true;
		unableToUpdatePrices = false;
		delay = calculateDelay(httpStatusCode, filterIsRequired, unableToUpdatePrices);
		assertEquals(0, delay);

		/**
		 * Min and Max values are considered to handle random 60 seconds addition to the
		 * result in the method.
		 */
		// case: Rate Limit error.
		httpStatusCode = TOO_MANY_REQUESTS_CODE;
		unableToUpdatePrices = true;
		delay = calculateDelay(httpStatusCode, filterIsRequired, unableToUpdatePrices);
		var minRateLimitDelay = 43200; // 12 hours
		var maxRateLimitDelay = 43260; // 12 hours + 60 seconds
		assertTrue(delay >= minRateLimitDelay && delay <= maxRateLimitDelay);

		// Case: server error
		httpStatusCode = SERVER_ERROR_CODE;
		unableToUpdatePrices = true;
		filterIsRequired = false;
		delay = calculateDelay(httpStatusCode, filterIsRequired, unableToUpdatePrices);
		var minDelay = 240; // 4 minutes and truncate to minutes is also considered.
		var maxDelay = 360; // 6 minutes
		assertTrue(delay >= minDelay && delay <= maxDelay);

		// case: no error.
		httpStatusCode = SUCCESS_CODE;
		unableToUpdatePrices = false;
		filterIsRequired = false;
		delay = calculateDelay(httpStatusCode, filterIsRequired, unableToUpdatePrices);
		var minSuccesDelay = 0; // 0 hours and truncate to hours is also considered.
		var maxSuccesDelay = 3660; // 1 hour
		assertTrue(delay >= minSuccesDelay && delay <= maxSuccesDelay);
	}

	@Test
	public void singleHomeParseTest() throws OpenemsNamedException {
		// Parsing with custom filter. Since the test is single home, the filter should
		// be ignored
		var prices = parsePrices(JSON_DATA, "tibber@openems.com"); //

		// To check if the Map is not empty
		assertFalse(prices.isEmpty());

		// To check if a value is present in map.
		assertEquals(0.2688 * 1000, prices.getFirst(), 0.001);
	}
}
