package io.openems.edge.controller.ess.timeofusetariff.v1;

import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.BALANCING;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.calculateChargeGridPower;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.calculateDelayDischargePower;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.postprocessRunState;
import static java.lang.Math.max;
import static java.util.stream.IntStream.concat;

import java.util.List;
import java.util.Objects;

import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.ess.emergencycapacityreserve.ControllerEssEmergencyCapacityReserve;
import io.openems.edge.controller.ess.limittotaldischarge.ControllerEssLimitTotalDischarge;
import io.openems.edge.controller.ess.timeofusetariff.StateMachine;
import io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffController;
import io.openems.edge.controller.ess.timeofusetariff.Utils.ApplyState;
import io.openems.edge.ess.api.ManagedSymmetricEss;

/**
 * Utils for {@link TimeOfUseTariffController}.
 * 
 * <p>
 * All energy values are in [Wh] and positive, unless stated differently.
 */
public final class UtilsV1 {

	private UtilsV1() {
	}

	public static final int PERIODS_PER_HOUR = 4;

	/**
	 * Returns the configured minimum SoC, or zero.
	 * 
	 * @param ctrlLimitTotalDischarges      the list of
	 *                                      {@link ControllerEssLimitTotalDischarge}
	 * @param ctrlEmergencyCapacityReserves the list of
	 *                                      {@link ControllerEssEmergencyCapacityReserve}
	 * @return the value in [%]
	 */
	public static int getEssMinSocPercentage(List<ControllerEssLimitTotalDischarge> ctrlLimitTotalDischarges,
			List<ControllerEssEmergencyCapacityReserve> ctrlEmergencyCapacityReserves) {
		return concat(//
				ctrlLimitTotalDischarges.stream() //
						.map(ctrl -> ctrl.getMinSoc().get()) //
						.filter(Objects::nonNull) //
						.mapToInt(v -> max(0, v)), // only positives
				ctrlEmergencyCapacityReserves.stream() //
						.map(ctrl -> ctrl.getActualReserveSoc().get()) //
						.filter(Objects::nonNull) //
						.mapToInt(v -> max(0, v))) // only positives
				.max().orElse(0);
	}

	/**
	 * Calculate Automatic Mode.
	 * 
	 * @param esh                        the {@link EnergyScheduleHandlerV1}
	 * @param sum                        the {@link Sum}
	 * @param ess                        the {@link ManagedSymmetricEss}
	 * @param maxChargePowerFromGrid     the configured max charge from grid power
	 * @param limitChargePowerFor14aEnWG Limit Charge Power for §14a EnWG
	 * @return {@link ApplyState}
	 */
	public static ApplyState calculateAutomaticMode(EnergyScheduleHandlerV1 esh, Sum sum, ManagedSymmetricEss ess,
			int maxChargePowerFromGrid, boolean limitChargePowerFor14aEnWG) {
		final var targetState = getCurrentPeriodState(esh);
		final var essChargeInChargeGrid = esh.getCurrentEssChargeInChargeGrid();
		return calculateAutomaticMode(sum, ess, essChargeInChargeGrid, maxChargePowerFromGrid,
				limitChargePowerFor14aEnWG, targetState);
	}

	/**
	 * Calculate Automatic Mode.
	 * 
	 * @param sum                        the {@link Sum}
	 * @param ess                        the {@link ManagedSymmetricEss}
	 * @param essChargeInChargeGrid      ESS Charge Energy in CHARGE_GRID State [Wh]
	 * @param maxChargePowerFromGrid     the configured max charge from grid power
	 * @param limitChargePowerFor14aEnWG Limit Charge Power for §14a EnWG
	 * @param targetState                the scheduled target {@link StateMachine}
	 * @return {@link ApplyState}
	 */
	protected static ApplyState calculateAutomaticMode(Sum sum, ManagedSymmetricEss ess, Integer essChargeInChargeGrid,
			int maxChargePowerFromGrid, boolean limitChargePowerFor14aEnWG, StateMachine targetState) {
		final StateMachine actualState;
		final Integer setPoint;

		var gridActivePower = sum.getGridActivePower().get(); // current buy-from/sell-to grid
		var essActivePower = ess.getActivePower().get(); // current charge/discharge ESS
		if (gridActivePower == null || essActivePower == null) {
			// undefined state
			return new ApplyState(BALANCING, null);
		}

		// Post-process and get actual state
		final var pwrBalancing = gridActivePower + essActivePower;
		final var pwrDelayDischarge = calculateDelayDischargePower(ess);
		final var pwrChargeGrid = calculateChargeGridPower(essChargeInChargeGrid, ess, essActivePower, gridActivePower,
				maxChargePowerFromGrid, limitChargePowerFor14aEnWG);
		actualState = postprocessRunState(targetState, pwrBalancing, pwrDelayDischarge, pwrChargeGrid);

		// Get and apply ActivePower Less-or-Equals Set-Point
		setPoint = switch (actualState) {
		case BALANCING -> null; // delegate to next priority Controller
		case DELAY_DISCHARGE -> pwrDelayDischarge;
		case CHARGE_GRID -> pwrChargeGrid;
		};

		return new ApplyState(actualState, setPoint);
	}

	/**
	 * Gets the current period state of the {@link EnergyScheduleHandlerV1} or
	 * {@link StateMachine#BALANCING}.
	 * 
	 * @param esh the {@link EnergyScheduleHandlerV1}
	 * @return the {@link StateMachine}
	 */
	public static StateMachine getCurrentPeriodState(EnergyScheduleHandlerV1 esh) {
		if (esh != null) {
			var state = esh.getCurrentState();
			if (state != null) {
				return state;
			}
		}
		return BALANCING; // Default Fallback
	}
}
