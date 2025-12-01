package io.openems.edge.energy.optimizer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SimulationResultTest {

	@Test
	public void test() {
		final var simulator = SimulatorTest.DUMMY_SIMULATOR;

		// ESH1 (BALANCING, DELAY_DISCHARGE, CHARGE_GRID)
		// ESH2 (FOO, BAR)
		final var mc = simulator.modeCombinations;
		assertEquals(6, mc.size());
		final var m00 = 0; // Defaults
		assertEquals(mc.getDefault(), mc.get(m00));
		assertEquals("BALANCING", mc.get(m00).mode(0).name());
		assertEquals("Controller.Dummy:FOO", mc.get(m00).mode(1).name());
		final var m11 = 3;
		assertEquals("DELAY_DISCHARGE", mc.get(m11).mode(0).name());
		assertEquals("Controller.Dummy:BAR", mc.get(m11).mode(1).name());
		final var m20 = 4;
		assertEquals("CHARGE_GRID", mc.get(m20).mode(0).name());
		assertEquals("Controller.Dummy:FOO", mc.get(m20).mode(1).name());
		final var m21 = 5;
		assertEquals("CHARGE_GRID", mc.get(m21).mode(0).name());
		assertEquals("Controller.Dummy:BAR", mc.get(m21).mode(1).name());

		var result = SimulationResult.fromQuarters(simulator.goc, new int[] { //
				m00, m11, m21, m00, m00, m00, m00, m00, m00, m00, m00, //
				m00, m00, m00, m11, m21, m00, m00, m00, m00, m00, m00, //
				m00, m00, m00, m00, m00, m11, m21, m00, m00, m00, m00, //
				m00, m00, m00, m00, m00, m00, m00, m11, m21, m00, m00, //
				m00, m00, m00, m00, m00, m00, m00, m00, m00, m11, m21, //
				m00, m00, m00, m00, m00, m00, m00, m00, m00, m00, m00, //
				m00, m11, m20 //
		});

		assertEquals(1165082.1, result.fitness().getGridBuyCost(), 0.1);
	}
}
