package io.openems.edge.energy.optimizer;

import static io.openems.edge.energy.api.simulation.GlobalOptimizationContext.PeriodDuration.QUARTER;
import static io.openems.edge.energy.optimizer.InitialPopulationUtils.generateInitialPopulation;
import static io.openems.edge.energy.optimizer.SimulatorTest.DUMMY_PREVIOUS_RESULT;
import static io.openems.edge.energy.optimizer.SimulatorTest.DUMMY_SIMULATOR;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.Test;

import io.openems.edge.energy.api.handler.DifferentModes.Period.Transition;

public class InitialPopulationUtilsTest {

	@Test
	public void testGenerateInitialPopulationNotFixed() {
		final var simulator = DUMMY_SIMULATOR;
		final var modeCombinations = ModeCombinations.fromGlobalOptimizationContext(simulator.goc);
		final var codec = EshCodec.of(simulator.goc, modeCombinations, DUMMY_PREVIOUS_RESULT, false);
		var schedules = generateInitialPopulation(codec).population();
		assertEquals(6, schedules.size());

		assertTrue(schedules.get(0).toString().startsWith("[[[0],[0],[0],[0],"));
		assertTrue(schedules.get(1).toString().startsWith("[[[3],[2],[1],[0],"));

		assertTrue(schedules.get(2).toString().startsWith("[[[1],[0],[0],[2],"));
		assertTrue(schedules.get(3).toString().startsWith("[[[1],[0],[0],[1],"));

		assertTrue(schedules.get(4).toString().startsWith("[[[1],[0],[0],[4],"));
		assertTrue(schedules.get(5).toString().startsWith("[[[1],[0],[0],[1],"));
	}

	@Test
	public void testGenerateInitialPopulationFixed() {
		final var simulator = DUMMY_SIMULATOR;
		final var modeCombinations = ModeCombinations.fromGlobalOptimizationContext(simulator.goc);
		final var codec = EshCodec.of(simulator.goc, modeCombinations, DUMMY_PREVIOUS_RESULT, true);
		var schedules = generateInitialPopulation(codec).population();
		assertEquals(6, schedules.size());

		assertTrue(schedules.get(0).toString().startsWith("[[[3],[0],[0],[0],"));
		assertTrue(schedules.get(1).toString().startsWith("[[[3],[2],[1],[0],"));

		assertTrue(schedules.get(2).toString().startsWith("[[[3],[0],[0],[2],"));
		assertTrue(schedules.get(3).toString().startsWith("[[[3],[0],[0],[1],"));

		assertTrue(schedules.get(4).toString().startsWith("[[[3],[0],[0],[4],"));
		assertTrue(schedules.get(5).toString().startsWith("[[[3],[0],[0],[1],"));
	}

	protected static Transition mode(int mode) {
		return new Transition(QUARTER, mode, 0., null, 0);
	}
}
