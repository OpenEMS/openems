package io.openems.edge.timeofusetariff.api.utils;

import static io.openems.edge.timeofusetariff.api.utils.TimeOfUseTariffUtils.generateDebugLog;
import static org.junit.Assert.assertEquals;

import java.time.Clock;

import org.junit.Test;

import io.openems.edge.common.currency.Currency;
import io.openems.edge.timeofusetariff.test.DummyTimeOfUseTariffProvider;

public class TimeOfUseTariffUtilsTest {

	@Test
	public void testGenerateDebugLog() {
		var prices = DummyTimeOfUseTariffProvider.fromQuarterlyPrices(Clock.systemDefaultZone(), //
				123.4567, 234.5678, 345.6789);

		assertEquals("Price:0.1235 EUR/kWh", generateDebugLog(prices, Currency.EUR));
		assertEquals("Price:0.1235 SEK/kWh", generateDebugLog(prices, Currency.SEK));

		assertEquals("Price:0.1235", generateDebugLog(prices, Currency.UNDEFINED));

		prices = DummyTimeOfUseTariffProvider.fromHourlyPrices(Clock.systemDefaultZone());
		assertEquals("Price:-", generateDebugLog(prices, Currency.UNDEFINED));
	}

}
