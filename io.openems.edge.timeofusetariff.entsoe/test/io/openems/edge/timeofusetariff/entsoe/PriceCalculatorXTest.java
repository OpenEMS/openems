package io.openems.edge.timeofusetariff.entsoe;

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import io.openems.common.timedata.DurationUnit;
import io.openems.common.utils.TimeRangeValues;
import io.openems.edge.timeofusetariff.entsoe.gridsell.GridSellUtils;

public class PriceCalculatorXTest {

	@Test
	public void testProcessPricesWithCalculator() {
		final var baseTime = ZonedDateTime.parse("2023-01-01T00:00:00Z");
		final var baseInstant = baseTime.toInstant();
		final var priceCalculator = new PriceCalculatorX("x * 1.19");

		var marketPrices = TimeRangeValues
				.builder(baseInstant, baseInstant.plus(45, MINUTES), DurationUnit.ofMinutes(15), Double.class) //
				.setByTime(baseInstant, 10.0)//
				.setByTime(baseInstant.plus(15, MINUTES), 20.0) //
				.setByTime(baseInstant.plus(30, MINUTES), 30.0) //
				.build();

		var result = GridSellUtils.processPrices(priceCalculator, marketPrices);

		assertEquals(11.9, result.getAt(baseInstant), 0.1);
		assertEquals(23.8, result.getAt(baseInstant.plus(15, MINUTES)), 0.1);
		assertEquals(35.7, result.getAt(baseInstant.plus(30, MINUTES)), 0.1);
	}

	@Test
	public void test() {
		// Division by zero
		assertTrue(Double.isNaN(new PriceCalculatorX("0 / x").calculate(0)));

		// Fallback to 'x'
		assertEquals(1, PriceCalculatorX.fromExpression(null).calculate(1));
		assertEquals(1, PriceCalculatorX.fromExpression("").calculate(1));
		assertEquals(1, PriceCalculatorX.fromExpression("   ").calculate(1));

		// 'y is not a number'
		assertThrows(IllegalArgumentException.class, () -> new PriceCalculatorX("x + y").calculate(1));

		// Expected results
		assertTrue(Double.isNaN(new PriceCalculatorX("x").calculate(Double.NaN)));
		assertEquals(1, new PriceCalculatorX("x").calculate(1));
		assertEquals(-4, new PriceCalculatorX("x * 2").calculate(-2));
	}
}
