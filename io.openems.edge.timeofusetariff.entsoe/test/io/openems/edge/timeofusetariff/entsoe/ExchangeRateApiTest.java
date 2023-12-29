package io.openems.edge.timeofusetariff.entsoe;

import static io.openems.edge.timeofusetariff.entsoe.ExchangeRateApi.getExchangeRate;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.currency.Currency;

public class ExchangeRateApiTest {

	private static final String RESPONSE = """
			{
				"success": true,
				"terms": "https://currencylayer.com/terms",
				"privacy": "https://currencylayer.com/privacy",
				"timestamp": 1699605243,
				"source": "EUR",
				"quotes": {
					"EURSEK": 11.649564
				}
			}
			""";

	// Remove '@Ignore' tag to test this API call.
	@Ignore
	@Test
	public void testGetExchangeRate() throws IOException, OpenemsNamedException {
		var rate = getExchangeRate(Token.parseExchangeRateAccesskeyOrNull(null), "EUR", Currency.SEK);
		System.out.println(rate);
	}

	@Test
	public void testParseResponse() throws OpenemsNamedException {
		var rate = ExchangeRateApi.parseResponse(RESPONSE, "EUR", Currency.SEK);
		assertEquals(11.649564, rate, 0.0001);
	}
}
