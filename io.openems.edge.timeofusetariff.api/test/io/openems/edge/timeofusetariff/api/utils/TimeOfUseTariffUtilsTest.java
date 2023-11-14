package io.openems.edge.timeofusetariff.api.utils;

import static io.openems.edge.timeofusetariff.api.utils.TimeOfUseTariffUtils.generateDebugLog;
import static org.junit.Assert.assertEquals;

import java.time.ZonedDateTime;

import org.junit.Test;

import io.openems.edge.common.currency.Currency;
import io.openems.edge.timeofusetariff.test.DummyTimeOfUseTariffProvider;

public class TimeOfUseTariffUtilsTest {

	@Test
	public void testGenerateDebugLog() {
		var now = ZonedDateTime.now();
		var prices = DummyTimeOfUseTariffProvider.hourlyPrices(now, 123.4567F, 234.5678F, 345.6789F);

		assertEquals("Price:123.46 €/kWh", generateDebugLog(prices, Currency.EUR));

		assertEquals("Price:123.46", generateDebugLog(prices, Currency.UNDEFINED));

		prices = DummyTimeOfUseTariffProvider.hourlyPrices(now);
		assertEquals("Price:-", generateDebugLog(prices, Currency.UNDEFINED));
	}

}
