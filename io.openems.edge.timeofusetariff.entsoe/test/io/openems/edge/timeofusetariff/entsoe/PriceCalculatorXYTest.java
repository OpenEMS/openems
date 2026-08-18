package io.openems.edge.timeofusetariff.entsoe;

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import io.openems.common.timedata.DurationUnit;
import io.openems.common.utils.TimeRangeValues;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;
import io.openems.edge.timeofusetariff.entsoe.gridbuy.GridBuyUtils;

// CHECKSTYLE:OFF
public class PriceCalculatorXYTest {
	// CHECKSTYLE:ON

	@Test
	public void testProcessPricesWithCalculator() {
		final var baseTime = ZonedDateTime.parse("2023-01-01T00:00:00Z");
		final var baseInstant = baseTime.toInstant();
		final var clock = Clock.fixed(baseInstant, baseTime.getZone());
		final var priceCalculator = new PriceCalculatorXY("(x + y) * 1.19");

		Double[] gridFees = { 1.0, 2.0, 3.0 }; // Length 3
		var gridFeesObject = TimeOfUsePrices.from(baseInstant, gridFees);
		var exchangeRate = 1.0;

		var marketPrices = TimeRangeValues
				.builder(baseInstant, baseInstant.plus(45, MINUTES), DurationUnit.ofMinutes(15), Double.class) //
				.setByTime(baseInstant, 10.0)//
				.setByTime(baseInstant.plus(15, MINUTES), 20.0) //
				.setByTime(baseInstant.plus(30, MINUTES), 30.0) //
				.build();

		var result = GridBuyUtils.processPrices(clock, priceCalculator, marketPrices, exchangeRate, gridFeesObject);

		assertEquals(23.8, result.getAt(baseInstant), 0.1);
		assertEquals(47.6, result.getAt(baseInstant.plus(15, MINUTES)), 0.1);
		assertEquals(71.4, result.getAt(baseInstant.plus(30, MINUTES)), 0.1);
	}

	@Test
	public void test() {
		// Division by zero
		assertTrue(Double.isNaN(new PriceCalculatorXY("x / y").calculate(0, 0)));

		// Fallback to 'x + y'
		assertEquals(2, PriceCalculatorXY.fromExpression(null).calculate(1, 1));
		assertEquals(2, PriceCalculatorXY.fromExpression("").calculate(1, 1));
		assertEquals(2, PriceCalculatorXY.fromExpression("   ").calculate(1, 1));

		// 'z is not a number'
		assertThrows(IllegalArgumentException.class, () -> new PriceCalculatorXY("x + y + z").calculate(1, 1));

		// Expected results
		assertTrue(Double.isNaN(new PriceCalculatorXY("x + y").calculate(Double.NaN, 1)));
		assertEquals(1, new PriceCalculatorXY("x / y").calculate(1, 1));
		assertEquals(2, new PriceCalculatorXY("x + y").calculate(1, 1));
		assertEquals(0, new PriceCalculatorXY("x - y").calculate(1, 1));
		assertEquals(1, new PriceCalculatorXY("x * y").calculate(1, 1));
		assertEquals(-3, new PriceCalculatorXY("x + y").calculate(-5, 2));
		assertEquals(-4, new PriceCalculatorXY("(x + y) * 2").calculate(-3, 1));

		// Repeated calls update variables on the same instance
		var reusedPriceCalculator = new PriceCalculatorXY("x - y");
		assertEquals(8, reusedPriceCalculator.calculate(10, 2));
		assertEquals(1, reusedPriceCalculator.calculate(3, 2));
	}
}
