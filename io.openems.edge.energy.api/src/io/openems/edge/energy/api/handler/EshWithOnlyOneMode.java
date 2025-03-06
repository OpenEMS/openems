package io.openems.edge.energy.api.handler;

import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.base.MoreObjects.ToStringHelper;

import io.openems.edge.energy.api.handler.OneMode.Simulator;
import io.openems.edge.energy.api.simulation.EnergyFlow;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;
import io.openems.edge.energy.api.simulation.GlobalScheduleContext;

public final class EshWithOnlyOneMode<OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> //
		extends AbstractEnergyScheduleHandler<OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> //
		implements EnergyScheduleHandler.WithOnlyOneMode {

	private final Simulator<OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> simulator;

	protected EshWithOnlyOneMode(//
			String id, //
			Function<GlobalOptimizationContext, OPTIMIZATION_CONTEXT> cocFunction, //
			Supplier<SCHEDULE_CONTEXT> cscSupplier, //
			Simulator<OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> simulator) {
		super(id, cocFunction, cscSupplier);
		this.simulator = simulator;
	}

	@SuppressWarnings("unchecked")
	@Override
	public final SCHEDULE_CONTEXT createScheduleContext() {
		return super.createScheduleContext();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void simulate(GlobalOptimizationContext.Period period, GlobalScheduleContext gsc, Object csc,
			EnergyFlow.Model ef) {
		this.simulator.simulate(period, gsc, this.coc, (SCHEDULE_CONTEXT) csc, ef);
	}

	@Override
	protected void buildToString(ToStringHelper toStringHelper) {
	}
}