package io.openems.edge.timeofusetariff.api.utils.calculator;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

// CHECKSTYLE:OFF
class PriceCalculatorXYTest {
	// CHECKSTYLE:ON

	@Test
	void test() {
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
