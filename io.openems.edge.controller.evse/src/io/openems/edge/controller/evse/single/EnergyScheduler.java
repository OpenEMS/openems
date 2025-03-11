package io.openems.edge.controller.evse.single;

import static io.openems.edge.common.type.TypeUtils.fitWithin;
import static io.openems.edge.energy.api.EnergyUtils.toEnergy;

import java.util.function.Supplier;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.handler.EshWithDifferentModes;
import io.openems.edge.energy.api.simulation.EnergyFlow;
import io.openems.edge.energy.api.simulation.GlobalScheduleContext;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint.ChargeParams;
import io.openems.edge.evse.api.chargepoint.Mode;

public class EnergyScheduler {

	public static record ManualOptimizationContext(Mode.Actual mode, boolean isReadyForCharging,
			ChargeParams chargeParams, int sessionEnergy, int sessionEnergyLimit) {
	}

	public static record SmartOptimizationContext(boolean isReadyForCharging) {
	}

	public static class ScheduleContext {
		private int sessionEnergy;

		public ScheduleContext(int initialSessionEnergy) {
			this.sessionEnergy = initialSessionEnergy;
		}

		protected void applyCharge(int chargeEnergy) {
			this.sessionEnergy += chargeEnergy;
		}
	}

	/**
	 * Builds the {@link EnergyScheduleHandler} for manual mode.
	 * 
	 * <p>
	 * This is public so that it can be used by the EnergyScheduler integration
	 * test.
	 * 
	 * @param parent      the parent {@link OpenemsComponent}
	 * @param cocSupplier supplier for ControllerOptimizationContext
	 * @return a {@link EnergyScheduleHandler}
	 */
	public static EnergyScheduleHandler.WithOnlyOneMode buildManualEnergyScheduleHandler(OpenemsComponent parent,
			Supplier<ManualOptimizationContext> cocSupplier) {
		return EnergyScheduleHandler.WithOnlyOneMode.<ManualOptimizationContext, ScheduleContext>create(parent) //
				.setOptimizationContext(gsc -> cocSupplier.get())
				.setScheduleContext(coc -> coc == null ? null : new ScheduleContext(coc.sessionEnergy)) //

				.setSimulator((period, gsc, coc, csc, ef) -> {
					if (coc == null || !coc.isReadyForCharging) {
						return;
					}

					// Evaluate Charge-Energy per mode
					var chargeEnergy = toEnergy(switch (coc.mode) {
					case FORCE -> coc.chargeParams.limit().getMaxPower();
					case MINIMUM -> coc.chargeParams.limit().getMinPower();
					case SURPLUS ->
						fitWithin(coc.chargeParams.limit().getMinPower(), coc.chargeParams.limit().getMaxPower(), //
								ef.production - ef.unmanagedConsumption);
					case ZERO -> 0;
					});
					if (chargeEnergy <= 0) {
						return; // stop early
					}

					// Apply Session Energy Limit
					chargeEnergy = Math.min(coc.sessionEnergyLimit - csc.sessionEnergy, chargeEnergy);

					if (chargeEnergy > 0) {
						ef.addConsumption(chargeEnergy);
						csc.applyCharge(chargeEnergy);
					}
				}) //

				.build();
	}

	/**
	 * Builds the {@link EnergyScheduleHandler} for {@link Mode#SMART} mode.
	 * 
	 * <p>
	 * This is public so that it can be used by the EnergyScheduler integration
	 * test.
	 * 
	 * @param parent      the parent {@link OpenemsComponent}
	 * @param cocSupplier supplier for ControllerOptimizationContext
	 * @return a {@link EnergyScheduleHandler}
	 */
	public static EshWithDifferentModes<Mode.Actual, SmartOptimizationContext, ScheduleContext> buildSmartEnergyScheduleHandler(
			OpenemsComponent parent, Supplier<SmartOptimizationContext> cocSupplier) {
		return EnergyScheduleHandler.WithDifferentModes.<Mode.Actual, SmartOptimizationContext, ScheduleContext>create(
				parent) //
				.setDefaultMode(Mode.Actual.MINIMUM) //
				.setAvailableModes(() -> Mode.Actual.values()).setOptimizationContext(gsc -> cocSupplier.get()) //
				.setScheduleContext(() -> new ScheduleContext(0 /* TODO */)) //

				.setSimulator((period, gsc, coc, csc, ef, mode) -> {
					return 0;
				})

				.setPostProcessor(EnergyScheduler::postprocessSimulatorState)

				.build();
	}

	/**
	 * Post-Process a state of a Period during Simulation, i.e. replace with
	 * 'better' state with the same behaviour.
	 * 
	 * <p>
	 * NOTE: heavy computation is ok here, because this method is called only at the
	 * end with the best Schedule.
	 * 
	 * @param gsc  the {@link GlobalScheduleContext}
	 * @param ef   the {@link EnergyFlow}
	 * @param coc  the {@link SmartOptimizationContext}
	 * @param mode the initial {@link Mode.Actual}
	 * @return the new Mode
	 */
	protected static Mode.Actual postprocessSimulatorState(GlobalScheduleContext gsc, EnergyFlow ef,
			SmartOptimizationContext coc, Mode.Actual mode) {
		if (mode == Mode.Actual.ZERO) {
			return mode;
		}
		if (ef.getManagedCons() == 0) { // TODO this works only reliably with one ControllerEvcs
			return Mode.Actual.ZERO;
		}
		return mode;
	}
}