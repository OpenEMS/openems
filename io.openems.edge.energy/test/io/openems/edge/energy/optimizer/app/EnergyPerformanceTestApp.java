package io.openems.edge.energy.optimizer.app;

import static io.openems.edge.energy.optimizer.SimulationResultTest.integerChromosomeOf;

import io.jenetics.Genotype;
import io.openems.edge.energy.api.simulation.OneSimulationContext;
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
		final var gsc = SimulatorTest.DUMMY_GSC;
		gsc.initializeEnergyScheduleHandlers();

		var osc = OneSimulationContext.from(gsc);
		var gt = Genotype.of(//
				// ESH1 (BALANCING, DELAY_DISCHARGE, CHARGE_GRID)
				integerChromosomeOf(1), //
				// ESH2 (FOO, BAR)
				integerChromosomeOf(1));

		Simulator.simulatePeriod(osc, gt, 0, null);
	}

}
