package io.openems.edge.timeofusetariff.tibber;

import static io.openems.edge.timeofusetariff.tibber.TimeOfUseTariffTibberImpl.CLIENT_ERROR_CODE;
import static io.openems.edge.timeofusetariff.tibber.TimeOfUseTariffTibberImpl.TOO_MANY_REQUESTS_CODE;
import static io.openems.edge.timeofusetariff.tibber.Utils.calculateDelay;
import static io.openems.edge.timeofusetariff.tibber.Utils.generateGraphQl;
import static io.openems.edge.timeofusetariff.tibber.Utils.parsePrices;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

public class UtilsTest {

	private static final int SUCCESS_CODE = 200;
	private static final int SERVER_ERROR_CODE = 500;

	private static String JSON_DATA = """
			{
			  "data": {
			    "viewer": {
			      "homes": [
			        {
			          "id": "foo-bar",
			          "appNickname": "my-alias",
			          "address": {
			            "country": "DE"
			          },
			          "currentSubscription": {
			            "priceInfo": {
			              "current": {
			                "total": 0.2466,
			                "energy": 0.0563,
			                "tax": 0.1903,
			                "startsAt": "2024-03-22T09:00:00.000+01:00"
			              },
			              "today": [
			                {
			                  "total": 0.2466,
			                  "energy": 0.0563,
			                  "tax": 0.1903,
			                  "startsAt": "2024-03-22T00:00:00.000+01:00"
			                },
			                {
			                  "total": 0.2417,
			                  "energy": 0.0522,
			                  "tax": 0.1895,
			                  "startsAt": "2024-03-22T01:00:00.000+01:00"
			                },
			                {
			                  "total": 0.2387,
			                  "energy": 0.0497,
			                  "tax": 0.189,
			                  "startsAt": "2024-03-22T02:00:00.000+01:00"
			                },
			                {
			                  "total": 0.2413,
			                  "energy": 0.0518,
			                  "tax": 0.1895,
			                  "startsAt": "2024-03-22T03:00:00.000+01:00"
			                },
			                {
			                  "total": 0.2424,
			                  "energy": 0.0528,
			                  "tax": 0.1896,
			                  "startsAt": "2024-03-22T04:00:00.000+01:00"
			                },
			                {
			                  "total": 0.2509,
			                  "energy": 0.0599,
			                  "tax": 0.191,
			                  "startsAt": "2024-03-22T05:00:00.000+01:00"
			                },
			                {
			                  "total": 0.2678,
			                  "energy": 0.0741,
			                  "tax": 0.1937,
			                  "startsAt": "2024-03-22T06:00:00.000+01:00"
			                },
			                {
			                  "total": 0.2598,
			                  "energy": 0.0674,
			                  "tax": 0.1924,
			                  "startsAt": "2024-03-22T07:00:00.000+01:00"
			                },
			                {
			                  "total": 0.2606,
			                  "energy": 0.0681,
			                  "tax": 0.1925,
			                  "startsAt": "2024-03-22T08:00:00.000+01:00"
			                },
			                {
			                  "total": 0.2466,
			                  "energy": 0.0563,
			                  "tax": 0.1903,
			                  "startsAt": "2024-03-22T09:00:00.000+01:00"
			                },
			                {
			                  "total": 0.2412,
			                  "energy": 0.0518,
			                  "tax": 0.1894,
			                  "startsAt": "2024-03-22T10:00:00.000+01:00"
			                },
			                {
			                  "total": 0.2389,
			                  "energy": 0.0498,
			                  "tax": 0.1891,
			                  "startsAt": "2024-03-22T11:00:00.000+01:00"
			                },
			                {
			                  "total": 0.2388,
			                  "energy": 0.0498,
			                  "tax": 0.189,
			                  "startsAt": "2024-03-22T12:00:00.000+01:00"
			                },
			                {
			                  "total": 0.2409,
			                  "energy": 0.0515,
			                  "tax": 0.1894,
			                  "startsAt": "2024-03-22T13:00:00.000+01:00"
			                },
			                {
			                  "total": 0.2419,
			                  "energy": 0.0524,
			                  "tax": 0.1895,
			                  "startsAt": "2024-03-22T14:00:00.000+01:00"
			                },
			                {
			                  "total": 0.2509,
			                  "energy": 0.06,
			                  "tax": 0.1909,
			                  "startsAt": "2024-03-22T15:00:00.000+01:00"
			                },
			                {
			                  "total": 0.2711,
			                  "energy": 0.0769,
			                  "tax": 0.1942,
			                  "startsAt": "2024-03-22T16:00:00.000+01:00"
			                },
			                {
			                  "total": 0.2855,
			                  "energy": 0.089,
			                  "tax": 0.1965,
			                  "startsAt": "2024-03-22T17:00:00.000+01:00"
			                },
			                {
			                  "total": 0.3243,
			                  "energy": 0.1216,
			                  "tax": 0.2027,
			                  "startsAt": "2024-03-22T18:00:00.000+01:00"
			                },
			                {
			                  "total": 0.3208,
			                  "energy": 0.1187,
			                  "tax": 0.2021,
			                  "startsAt": "2024-03-22T19:00:00.000+01:00"
			                },
			                {
			                  "total": 0.2926,
			                  "energy": 0.095,
			                  "tax": 0.1976,
			                  "startsAt": "2024-03-22T20:00:00.000+01:00"
			                },
			                {
			                  "total": 0.2775,
			                  "energy": 0.0822,
			                  "tax": 0.1953,
			                  "startsAt": "2024-03-22T21:00:00.000+01:00"
			                },
			                {
			                  "total": 0.275,
			                  "energy": 0.0802,
			                  "tax": 0.1948,
			                  "startsAt": "2024-03-22T22:00:00.000+01:00"
			                },
			                {
			                  "total": 0.2718,
			                  "energy": 0.0775,
			                  "tax": 0.1943,
			                  "startsAt": "2024-03-22T23:00:00.000+01:00"
			                }
			              ],
			              "tomorrow": []
			            }
			          }
			        }
			      ]
			    }
			  }
			}
			""";

	@Test
	public void nonEmptyStringTest() throws OpenemsNamedException {
		// Parsing with custom data
		var prices = parsePrices(JSON_DATA, null); //

		// To check if the Map is not empty
		assertFalse(prices.isEmpty());

		// To check if a value is present in map.
		assertEquals(0.2466 * 1000, prices.getFirst(), 0.001);

		// To check 15 minutes values are taken instead of one hour values.
		var firstHour = prices.pricePerQuarter.firstKey();
		assertTrue(prices.pricePerQuarter.containsKey(firstHour.plusMinutes(15)));
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
				        priceInfo{
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
		assertEquals(0.2466 * 1000, prices.getFirst(), 0.001);
	}
}
