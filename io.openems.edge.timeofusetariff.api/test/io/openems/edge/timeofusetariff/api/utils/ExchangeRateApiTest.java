package io.openems.edge.timeofusetariff.api.utils;

import static io.openems.edge.timeofusetariff.api.utils.ExchangeRateApi.getExchangeRate;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Ignore;
import org.junit.Test;
import org.xml.sax.SAXException;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.currency.Currency;

public class ExchangeRateApiTest {

	private static final String RESPONSE = """
			<gesmes:Envelope xmlns:gesmes="http://www.gesmes.org/xml/2002-08-01" xmlns="http://www.ecb.int/vocabulary/2002-08-01/eurofxref">
				<gesmes:subject>Reference rates</gesmes:subject>
				<gesmes:Sender>
					<gesmes:name>European Central Bank</gesmes:name>
				</gesmes:Sender>
				<Cube>
					<Cube time="2024-10-25">
						<Cube currency="USD" rate="1.0825"/>
						<Cube currency="JPY" rate="164.45"/>
						<Cube currency="BGN" rate="1.9558"/>
						<Cube currency="CZK" rate="25.250"/>
						<Cube currency="DKK" rate="7.4609"/>
						<Cube currency="GBP" rate="0.83358"/>
						<Cube currency="HUF" rate="404.68"/>
						<Cube currency="PLN" rate="4.3478"/>
						<Cube currency="RON" rate="4.9733"/>
						<Cube currency="SEK" rate="11.4475"/>
						<Cube currency="CHF" rate="0.9382"/>
						<Cube currency="ISK" rate="149.10"/>
						<Cube currency="NOK" rate="11.8195"/>
						<Cube currency="TRY" rate="37.1180"/>
						<Cube currency="AUD" rate="1.6311"/>
						<Cube currency="BRL" rate="6.1420"/>
						<Cube currency="CAD" rate="1.4989"/>
						<Cube currency="CNY" rate="7.7123"/>
						<Cube currency="HKD" rate="8.4110"/>
						<Cube currency="IDR" rate="16978.90"/>
						<Cube currency="ILS" rate="4.1062"/>
						<Cube currency="INR" rate="91.0270"/>
						<Cube currency="KRW" rate="1504.09"/>
						<Cube currency="MXN" rate="21.4408"/>
						<Cube currency="MYR" rate="4.7024"/>
						<Cube currency="NZD" rate="1.8025"/>
						<Cube currency="PHP" rate="63.118"/>
						<Cube currency="SGD" rate="1.4290"/>
						<Cube currency="THB" rate="36.540"/>
						<Cube currency="ZAR" rate="19.0627"/>
					</Cube>
				</Cube>
			</gesmes:Envelope>
						""";

	// Remove '@Ignore' tag to test this API call.
	@Ignore
	@Test
	public void testGetExchangeRate() throws IOException, OpenemsNamedException, ParserConfigurationException, SAXException {
		var rate = getExchangeRate("EUR", Currency.SEK);
		System.out.println(rate);
	}

	@Test
	public void testParseResponse()
			throws OpenemsNamedException, ParserConfigurationException, SAXException, IOException {
		var rate = ExchangeRateApi.parseResponse(RESPONSE, Currency.SEK);
		assertEquals(11.4475, rate, 0.0001);
	}
}
