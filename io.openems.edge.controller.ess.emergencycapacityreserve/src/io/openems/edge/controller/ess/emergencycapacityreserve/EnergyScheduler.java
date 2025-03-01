package io.openems.edge.controller.ess.emergencycapacityreserve;

import static io.openems.edge.energy.api.EnergyUtils.socToEnergy;
import static java.lang.Math.max;

import java.util.function.Supplier;

import io.openems.edge.energy.api.handler.EnergyScheduleHandler;

public class EnergyScheduler {

	private static record OptimizationContext(int minSoc) {
	}

	/**
	 * Builds the {@link EnergyScheduleHandler}.
	 * 
	 * <p>
	 * This is public so that it can be used by the EnergyScheduler integration
	 * test.
	 * 
	 * @param componentId    supplier for parent Component-ID
	 * @param minSocSupplier supplier for the configured minSoc
	 * @return a {@link EnergyScheduleHandler}
	 */
	public static EnergyScheduleHandler.WithOnlyOneMode buildEnergyScheduleHandler(Supplier<String> componentId,
			Supplier<Integer> minSocSupplier) {
		return EnergyScheduleHandler.WithOnlyOneMode.<OptimizationContext, Void>create() //
				.setComponentId(componentId.get()) //

				.setOptimizationContext(gsc -> {
					var minSoc = minSocSupplier.get();
					return minSoc != null //
							? new OptimizationContext(socToEnergy(gsc.ess().totalEnergy(), minSoc)) //
							: null; //
				})

				.setSimulator((gsc, coc, ef) -> {
					if (coc != null) {
						ef.setEssMaxDischarge(max(0, gsc.ess.getInitialEnergy() - coc.minSoc));
					}
				}) //

				.build();
	}
}
