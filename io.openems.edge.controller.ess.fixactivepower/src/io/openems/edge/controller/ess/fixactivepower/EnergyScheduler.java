package io.openems.edge.controller.ess.fixactivepower;

import java.util.function.Supplier;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.ess.power.api.Relationship;

public class EnergyScheduler {

	public static record OptimizationContext(int energy, Relationship relationship) {
	}

	/**
	 * Builds the {@link EnergyScheduleHandler}.
	 * 
	 * <p>
	 * This is public so that it can be used by the EnergyScheduler integration
	 * test.
	 * 
	 * @param parent      the parent {@link OpenemsComponent}
	 * @param cocSupplier supplier for {@link OptimizationContext}
	 * @return a {@link EnergyScheduleHandler}
	 */
	public static EnergyScheduleHandler.WithOnlyOneMode buildEnergyScheduleHandler(OpenemsComponent parent,
			Supplier<OptimizationContext> cocSupplier) {
		return EnergyScheduleHandler.WithOnlyOneMode.<OptimizationContext, Void>create(parent) //
				.setOptimizationContext(() -> cocSupplier.get()) //

				.setSimulator((gsc, coc, ef) -> {
					if (coc != null) {
						switch (coc.relationship) {
						case EQUALS -> ef.setEss(coc.energy);
						case GREATER_OR_EQUALS -> ef.setEssMaxCharge(-coc.energy);
						case LESS_OR_EQUALS -> ef.setEssMaxDischarge(coc.energy);
						}
					}
				}) //

				.build();
	}

}