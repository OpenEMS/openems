package io.openems.edge.controller.ess.limittotaldischarge;

import static io.openems.edge.energy.api.EnergyUtils.socToEnergy;
import static java.lang.Math.max;

import java.util.function.Supplier;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;

public class EnergyScheduler {

	public static record OptimizationContext(int minEnergy) {
	}

	/**
	 * Builds the {@link EnergyScheduleHandler}.
	 * 
	 * <p>
	 * This is public so that it can be used by the EnergyScheduler integration
	 * test.
	 * 
	 * @param parent         the parent {@link OpenemsComponent}
	 * @param minSocSupplier supplier for the configured minSoc
	 * @return a {@link EnergyScheduleHandler}
	 */
	public static EnergyScheduleHandler.WithOnlyOneMode buildEnergyScheduleHandler(OpenemsComponent parent,
			Supplier<Integer> minSocSupplier) {
		return EnergyScheduleHandler.WithOnlyOneMode.<OptimizationContext, Void>create(parent) //
				.setOptimizationContext(gsc -> {
					var minSoc = minSocSupplier.get();
					return minSoc != null //
							? new OptimizationContext(socToEnergy(gsc.ess().totalEnergy(), minSoc)) //
							: null; //
				})

				.setSimulator((gsc, coc, ef) -> {
					if (coc != null) {
						ef.setEssMaxDischarge(max(0, gsc.ess.getInitialEnergy() - coc.minEnergy));
					}
				}) //

				.build();
	}
}