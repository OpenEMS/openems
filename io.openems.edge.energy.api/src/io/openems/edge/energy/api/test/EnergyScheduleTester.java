package io.openems.edge.energy.api.test;

import com.google.common.collect.ImmutableList;

import io.openems.edge.energy.api.handler.AbstractEnergyScheduleHandler;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.simulation.EnergyFlow;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;
import io.openems.edge.energy.api.simulation.GlobalScheduleContext;

public class EnergyScheduleTester {

	public final GlobalOptimizationContext goc;
	public final ImmutableList<Object> cscs;

	/**
	 * Builds an {@link EnergyScheduleTester}.
	 * 
	 * @param eshs the {@link EnergyScheduleHandler}s
	 * @return the {@link EnergyScheduleTester}
	 */
	public static EnergyScheduleTester from(EnergyScheduleHandler... eshs) {
		var cscs = ImmutableList.<Object>builder();
		var goc = DummyGlobalOptimizationContext.fromHandlers(eshs);
		for (var esh : eshs) {
			var csc = ((AbstractEnergyScheduleHandler<?, ?>) esh /* this is safe */).initialize(goc);
			if (csc != null) {
				cscs.add(csc);
			}
		}
		return new EnergyScheduleTester(goc, cscs.build());
	}

	private EnergyScheduleTester(GlobalOptimizationContext goc, ImmutableList<Object> cscs) {
		this.goc = goc;
		this.cscs = cscs;
	}

	public static record SimulatedPeriod(GlobalOptimizationContext.Period period, GlobalScheduleContext gsc,
			EnergyFlow.Model ef, double cost) {
	}

	/**
	 * Test one Period.
	 * 
	 * @param index the index of the Period
	 * @param modes simulated Modes
	 * @return a {@link SimulatedPeriod} record
	 */
	public SimulatedPeriod simulatePeriod(int index, int... modes) {
		var gsc = GlobalScheduleContext.from(this.goc);
		var period = this.goc.periods().get(index);
		var ef = EnergyFlow.Model.from(gsc, period);

		double cost = 0.;
		var eshIndex = 0;
		for (var esh : this.goc.eshs()) {
			switch (esh) {
			case EnergyScheduleHandler.WithDifferentModes e -> {
				var modeIndex = eshIndex++;
				if (modeIndex >= modes.length) {
					throw new IllegalArgumentException("Missing ModeIndex [" + modeIndex + "] for " + esh);
				}
				cost += e.simulate(period, gsc, null, ef, modes[modeIndex]);
			}
			case EnergyScheduleHandler.WithOnlyOneMode e //
				-> e.simulate(period, gsc, null, ef);
			}
		}

		return new SimulatedPeriod(period, gsc, ef, cost);
	}
}
