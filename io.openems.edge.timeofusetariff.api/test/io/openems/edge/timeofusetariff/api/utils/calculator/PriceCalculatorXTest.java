package io.openems.edge.timeofusetariff.api.utils.calculator;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PriceCalculatorXTest {
	@Test
	void test() {
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
