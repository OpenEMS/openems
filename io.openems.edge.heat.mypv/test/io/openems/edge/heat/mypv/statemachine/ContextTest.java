package io.openems.edge.heat.mypv.statemachine;

import static io.openems.edge.heat.mypv.statemachine.Context.calculateSurplusTargetActivePower;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ContextTest {

	@Nested
	class CalculateSurplusTargetActivePower {

		// gridActivePower, activePower, essDischargePower, maxHeatPower, expected
		@ParameterizedTest(name = "grid={0}, active={1}, ess={2}, max={3} -> {4}")
		@CsvSource({
				// surplus below max: result equals surplus minus ESS discharge
				"-1500, 200, 300, 3000, 1400",
				// surplus above max: result is capped at maxHeatPower
				"-5000, 0, 0, 3000, 3000",
				// no surplus (positive grid): result is zero, never negative
				"500, 0, 0, 3000, 0",
				// surplus exactly zero: result is zero
				"0, 0, 0, 3000, 0",
				// ESS discharge matches surplus exactly: result is zero
				"-1000, 0, 1000, 3000, 0",
				// surplus below max, no ESS discharge: result equals surplus
				"-2000, 0, 0, 3000, 2000", //
				"-2000, 0, -2000, 3000, 2000", //
				"-2000, 500, -2000, 3000, 2500", //
				"-2000, 500, 2000, 3000, 500", //
		})
		void calculatesSurplusCorrectly(//
				int gridActivePower, //
				int activePower, //
				int essDischargePower, //
				int maxHeatPower, //
				int expected) { //
			assertEquals(expected, //
					calculateSurplusTargetActivePower(//
							gridActivePower, //
							activePower, //
							essDischargePower, //
							maxHeatPower));
		}
	}

}