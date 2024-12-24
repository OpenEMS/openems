package io.openems.edge.energy.optimizer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SimulationResultTest {

	@Test
	public void test() {
		final var simulator = SimulatorTest.DUMMY_SIMULATOR;

		// ESH1 (BALANCING, DELAY_DISCHARGE, CHARGE_GRID)
		// ESH2 (FOO, BAR)
		var result = SimulationResult.fromQuarters(simulator.gsc, new int[][] { //
				p(0, 0), p(1, 1), p(2, 1), p(0, 0), p(0, 0), p(0, 0), p(0, 0), p(0, 0), p(0, 0), p(0, 0), p(0, 0),
				p(0, 0), p(0, 0), p(0, 0), p(1, 1), p(2, 1), p(0, 0), p(0, 0), p(0, 0), p(0, 0), p(0, 0), p(0, 0),
				p(0, 0), p(0, 0), p(0, 0), p(0, 0), p(0, 0), p(1, 1), p(2, 1), p(0, 0), p(0, 0), p(0, 0), p(0, 0),
				p(0, 0), p(0, 0), p(0, 0), p(0, 0), p(0, 0), p(0, 0), p(0, 0), p(1, 1), p(2, 1), p(0, 0), p(0, 0),
				p(0, 0), p(0, 0), p(0, 0), p(0, 0), p(0, 0), p(0, 0), p(0, 0), p(0, 0), p(0, 0), p(1, 1), p(2, 1),
				p(0, 0), p(0, 0), p(0, 0), p(0, 0), p(0, 0), p(0, 0), p(0, 0), p(0, 0), p(0, 0), p(0, 0), p(0, 0),
				p(0, 0), p(1, 1), p(2, 0) //
		});

		assertEquals(1166163.462, result.cost(), 0.001);
	}

	private static int[] p(int... states) {
		return states;
	}
}
