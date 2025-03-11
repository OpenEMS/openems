package io.openems.edge.controller.evse.single;

import java.util.function.Supplier;

import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.Mode;

public class EnergyScheduler {

	public static sealed interface OptimizationContext {

		public static record Manual(Mode.Actual mode, boolean isReadyForCharging) implements OptimizationContext {

			/**
			 * Factory for {@link EshManualContext} from {@link Config}.
			 * 
			 * @param chargePoint the {@link EvseChargePoint}
			 * @param mode        the {@link Mode.Actual}
			 * @return the {@link EshManualContext}
			 */
			public static Manual from(EvseChargePoint chargePoint, Mode.Actual mode) {
				var isReadyForCharging = switch (chargePoint.getStatus()) {
				case CHARGING, READY_FOR_CHARGING //
					-> true;
				case CHARGING_REJECTED, ENERGY_LIMIT_REACHED, ERROR, NOT_READY_FOR_CHARGING, STARTING, UNDEFINED //
					-> false;
				};
				return new Manual(mode, isReadyForCharging);
			}
		}
	}

	/**
	 * Builds the {@link EnergyScheduleHandler}.
	 * 
	 * <p>
	 * This is public so that it can be used by the EnergyScheduler integration
	 * test.
	 * 
	 * @param componentId supplier for parent Component-ID
	 * @param cocSupplier supplier for ControllerOptimizationContext
	 * @return a {@link EnergyScheduleHandler}
	 */
	public static EnergyScheduleHandler.WithOnlyOneMode buildManualEnergyScheduleHandler(Supplier<String> componentId,
			Supplier<OptimizationContext.Manual> cocSupplier) {
		return EnergyScheduleHandler.WithOnlyOneMode.<OptimizationContext, Void>create() //
				.setComponentId(componentId.get()) //

				.setOptimizationContext(gsc -> cocSupplier.get())

				.setSimulator((gsc, coc, ef) -> {
					// TODO
				}) //

				.build();
	}
}