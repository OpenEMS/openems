package io.openems.edge.energy.api.handler;

import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.gson.JsonElement;

import io.openems.edge.energy.api.handler.OneMode.Simulator;
import io.openems.edge.energy.api.simulation.EnergyFlow;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;
import io.openems.edge.energy.api.simulation.GlobalScheduleContext;

public final class EshWithOnlyOneMode<OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> //
		extends AbstractEnergyScheduleHandler<OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> //
		implements EnergyScheduleHandler.WithOnlyOneMode {

	private final Simulator<OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> simulator;

	protected EshWithOnlyOneMode(//
			String parentFactoryPid, String parentId, //
			Supplier<JsonElement> serializer, //
			Function<GlobalOptimizationContext, OPTIMIZATION_CONTEXT> cocFunction, //
			Function<OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> cscFunction, //
			Simulator<OPTIMIZATION_CONTEXT, SCHEDULE_CONTEXT> simulator) {
		super(parentFactoryPid, parentId, serializer, cocFunction, cscFunction);
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
			EnergyFlow.Model ef, Fitness fitness) {
		this.simulator.simulate(this.getParentId(), period, gsc, this.coc, (SCHEDULE_CONTEXT) csc, ef, fitness);
	}

	@Override
	protected void buildToString(ToStringHelper toStringHelper) {
	}
}