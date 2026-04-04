package io.openems.edge.solaredge.ess;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AverageCalculatorTest {

	@Test
	public void test() throws Exception {
		var pvProductionAverageCalculator = new AverageCalculator(120); // use 120 average values
		
		for (int i = 0; i < 60; i++) {
			pvProductionAverageCalculator.addValue(100);
			pvProductionAverageCalculator.addValue(200);
		}
		
		assertEquals(150, pvProductionAverageCalculator.getAverage());
	}
}