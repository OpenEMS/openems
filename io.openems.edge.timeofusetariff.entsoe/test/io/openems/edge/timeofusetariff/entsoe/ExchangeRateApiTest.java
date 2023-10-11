package io.openems.edge.timeofusetariff.entsoe;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.currency.Currency;

public class ExchangeRateApiTest {

	private static final String EXCHANGE_DATA = """
			{
				"success": true,
				"source": "EUR",
				"date": "2023-07-27",
				"quotes": {
					"EURAED": 3.894874,
					"EURAFN": 79.984782,
			     	"EURALL": 105.884804,
			     	"EURAMD": 421.63859,
				    "EURANG": 1.911328,
				    "EURAOA": 877.465407,
				    "EURSCR": 13.852121,
				    "EURSDG": 637.697611,
				    "EURSEK": 11.562427,
				    "EURSGD": 1.445276,
				    "EURSHP": 1.290222,
				    "EURZWL": 341.443067
				}
			}
			""";

	@Test
	@Ignore
	public void testExchangeRateApi() throws IOException {
		// Enter personal access key and remove '@Ignore' tag to test it.
		var accessKey = "";
		ExchangeRateApi.getExchangeRates(accessKey);
	}

	@Test
	public void testExchangeRateParser() throws OpenemsNamedException {

		var currency = Currency.SEK;
		var response = Utils.exchangeRateParser(EXCHANGE_DATA, currency);

		assertTrue(response == 11.562427);
	}
}
