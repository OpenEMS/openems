package io.openems.edge.timeofusetariff.entsoe;

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.ZonedDateTime;

import org.junit.Test;

import io.openems.common.timedata.DurationUnit;
import io.openems.common.utils.TimeRangeValues;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;

public class PriceCalculatorTest {

	@Test
	public void testProcessPricesWithCalculator() {
		final var baseTime = ZonedDateTime.parse("2023-01-01T00:00:00Z");
		final var baseInstant = baseTime.toInstant();
		final var clock = Clock.fixed(baseInstant, baseTime.getZone());
		final var priceCalculator = new PriceCalculator("(x + y) * 1.19");

		Double[] gridFees = { 1.0, 2.0, 3.0 }; // Length 3
		var gridFeesObject = TimeOfUsePrices.from(baseInstant, gridFees);
		var exchangeRate = 1.0;

		var marketPrices = TimeRangeValues
				.builder(baseInstant, baseInstant.plus(45, MINUTES), DurationUnit.ofMinutes(15), Double.class) //
				.setByTime(baseInstant, 10.0)//
				.setByTime(baseInstant.plus(15, MINUTES), 20.0) //
				.setByTime(baseInstant.plus(30, MINUTES), 30.0) //
				.build();

		var result = Utils.processPrices(clock, priceCalculator, marketPrices, exchangeRate, gridFeesObject);

		assertEquals(23.8, result.getAt(baseInstant), 0.1);
		assertEquals(47.6, result.getAt(baseInstant.plus(15, MINUTES)), 0.1);
		assertEquals(71.4, result.getAt(baseInstant.plus(30, MINUTES)), 0.1);
	}
}
