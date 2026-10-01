package io.openems.edge.heat.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class RemainingHeatEnergyCalculatorTest {

	@Test
	void returnsUndefinedWhenActualTemperatureIsMissing() {
		assertNull(RemainingHeatEnergyCalculator.calculate(400, null, 700));
	}

	@Test
	void returnsUndefinedWhenTargetTemperatureIsMissing() {
		assertNull(RemainingHeatEnergyCalculator.calculate(400, 644, null));
	}

	@Test
	void calculatesRemainingEnergyFromDeciDegreesAndRoundsToWh() {
		assertEquals(2605, RemainingHeatEnergyCalculator.calculate(400, 644, 700));
	}

	@Test
	void returnsZeroWhenActualTemperatureEqualsTargetTemperature() {
		assertEquals(0, RemainingHeatEnergyCalculator.calculate(400, 700, 700));
	}

	@Test
	void returnsZeroWhenActualTemperatureExceedsTargetTemperature() {
		assertEquals(0, RemainingHeatEnergyCalculator.calculate(400, 710, 700));
	}
}
