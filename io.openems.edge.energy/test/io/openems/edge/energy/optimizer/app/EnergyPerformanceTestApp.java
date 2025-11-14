package io.openems.edge.energy.optimizer.app;

import com.google.common.collect.ImmutableMap;

import io.openems.edge.energy.api.handler.EnergyScheduleHandler.Fitness;
import io.openems.edge.energy.api.simulation.GlobalScheduleContext;
import io.openems.edge.energy.optimizer.Simulator;
import io.openems.edge.energy.optimizer.SimulatorTest;

public class EnergyPerformanceTestApp {

	/**
	 * Runs a performance test.
	 * 
	 * @param args the args
	 */
	public static void main(String[] args) {
		// ~ 20s for 100000 EnergyFlow
		// - 0.5s for 100000 Math.Random

		long startTime = System.currentTimeMillis();

		for (var i = 0; i < 100000; i++) {
			simulatePeriod();
		}

		long finishTime = System.currentTimeMillis();
		System.out.println("That took: " + (finishTime - startTime) + " ms");
	}

	private static void simulatePeriod() {
		final var simulator = SimulatorTest.DUMMY_SIMULATOR;
		final var modeCombination = simulator.modeCombinations.getDefault();

		var gsc = GlobalScheduleContext.from(simulator.goc);

		Simulator.simulatePeriod(gsc, ImmutableMap.of(), 0 /* period */, modeCombination, new Fitness(), null);
	}

}
