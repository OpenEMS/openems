package io.openems.edge.controller.ess.timeofusetariff.v1;

import static io.openems.edge.controller.ess.limiter14a.ControllerEssLimiter14a.ESS_LIMIT_14A_ENWG;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.BALANCING;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.CHARGE_GRID;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.DELAY_DISCHARGE;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.ESS_CHARGE_C_RATE;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.round;
import static java.util.stream.IntStream.concat;

import java.util.List;
import java.util.Objects;

import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.ess.emergencycapacityreserve.ControllerEssEmergencyCapacityReserve;
import io.openems.edge.controller.ess.limiter14a.ControllerEssLimiter14a;
import io.openems.edge.controller.ess.limittotaldischarge.ControllerEssLimitTotalDischarge;
import io.openems.edge.controller.ess.timeofusetariff.StateMachine;
import io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffController;
import io.openems.edge.controller.ess.timeofusetariff.Utils.ApplyMode;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;

/**
 * Utils for {@link TimeOfUseTariffController}.
 * 
 * <p>
 * All energy values are in [Wh] and positive, unless stated differently.
 */
@Deprecated
public final class UtilsV1 {

	private UtilsV1() {
	}

	@Deprecated
	public static final int PERIODS_PER_HOUR = 4;

	/** Keep some buffer to avoid scheduling errors because of bad predictions. */
	@Deprecated
	public static final float ESS_MAX_SOC = 94F;

	/**
	 * Returns the configured minimum SoC, or zero.
	 * 
	 * @param ctrlLimitTotalDischarges      the list of
	 *                                      {@link ControllerEssLimitTotalDischarge}
	 * @param ctrlEmergencyCapacityReserves the list of
	 *                                      {@link ControllerEssEmergencyCapacityReserve}
	 * @return the value in [%]
	 */
	@Deprecated
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
	 * @param esh                    the {@link EnergyScheduleHandlerV1}
	 * @param sum                    the {@link Sum}
	 * @param ess                    the {@link ManagedSymmetricEss}
	 * @param ctrlLimiter14as        the list of {@link ControllerEssLimiter14a}s
	 * @param maxChargePowerFromGrid the configured max charge from grid power
	 * @param forceState             force a target {@link StateMachine}
	 * @return {@link ApplyMode}
	 */
	@Deprecated
	public static ApplyMode calculateAutomaticMode(EnergyScheduleHandlerV1 esh, Sum sum, ManagedSymmetricEss ess,
			List<ControllerEssLimiter14a> ctrlLimiter14as, int maxChargePowerFromGrid, StateMachine forceState) {
		final var targetState = getCurrentPeriodState(esh);
		final var essChargeInChargeGrid = esh.getCurrentEssChargeInChargeGrid();
		final var limitChargePowerFor14aEnWG = calculateLimitChargePowerFor14aEnWG(ctrlLimiter14as);
		return calculateAutomaticMode(sum, ess, essChargeInChargeGrid, maxChargePowerFromGrid,
				limitChargePowerFor14aEnWG, targetState, forceState);
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
	 * @param forceState                 force a target {@link StateMachine}
	 * @return {@link ApplyMode}
	 */
	@Deprecated
	protected static ApplyMode calculateAutomaticMode(Sum sum, ManagedSymmetricEss ess, Integer essChargeInChargeGrid,
			int maxChargePowerFromGrid, int limitChargePowerFor14aEnWG, StateMachine targetState,
			StateMachine forceState) {
		var gridActivePower = sum.getGridActivePower().get(); // current buy-from/sell-to grid
		var essActivePower = ess.getActivePower().get(); // current charge/discharge ESS
		if (gridActivePower == null || essActivePower == null) {
			// undefined state
			return new ApplyMode(BALANCING, null);
		}

		// Post-process and get actual state
		final var pwrBalancing = gridActivePower + essActivePower;
		final var pwrDelayDischarge = calculateDelayDischargePower(ess);
		final var pwrChargeGrid = max(limitChargePowerFor14aEnWG, calculateChargeGridPower(//
				essChargeInChargeGrid, ess, essActivePower, gridActivePower, maxChargePowerFromGrid));
		final var actualState = forceState != null //
				? forceState //
				: postprocessRunState(ess, targetState, pwrBalancing, pwrDelayDischarge, pwrChargeGrid);

		// Get and apply ActivePower Less-or-Equals Set-Point
		final var setPoint = switch (actualState) {
		case BALANCING -> null; // delegate to next priority Controller
		case DELAY_DISCHARGE -> pwrDelayDischarge;
		case CHARGE_GRID -> pwrChargeGrid;
		case DISCHARGE_GRID, PEAK_SHAVING -> null; // NOT IMPLEMENTED
		};

		return new ApplyMode(actualState, setPoint);
	}

	/**
	 * Gets the current period state of the {@link EnergyScheduleHandlerV1} or
	 * {@link StateMachine#BALANCING}.
	 * 
	 * @param esh the {@link EnergyScheduleHandlerV1}
	 * @return the {@link StateMachine}
	 */
	@Deprecated
	public static StateMachine getCurrentPeriodState(EnergyScheduleHandlerV1 esh) {
		if (esh != null) {
			var state = esh.getCurrentState();
			if (state != null) {
				return state;
			}
		}
		return BALANCING; // Default Fallback
	}

	/**
	 * Calculates the limit for §14a EnWG.
	 * 
	 * @param ctrlLimiter14as the list of {@link ControllerEssLimiter14a}s
	 * @return the (negative) charge value or {@link Integer#MIN_VALUE} for no limit
	 */
	@Deprecated
	public static int calculateLimitChargePowerFor14aEnWG(List<ControllerEssLimiter14a> ctrlLimiter14as) {
		var isLimited = ctrlLimiter14as.stream() //
				.map(c -> c.getRestrictionMode()) //
				.anyMatch(r -> r == Boolean.TRUE);
		if (!isLimited) {
			return Integer.MIN_VALUE;
		}
		return ESS_LIMIT_14A_ENWG; // 4.2 kW
	}

	/**
	 * Calculates the Max-ActivePower constraint for
	 * {@link StateMachine#CHARGE_GRID}.
	 * 
	 * @param essChargePowerInChargeGrid ESS Charge Power in CHARGE_GRID State [W]
	 * @param ess                        the {@link ManagedSymmetricEss}
	 * @param essActivePower             the ESS ActivePower
	 * @param gridActivePower            the Grid ActivePower
	 * @param maxChargePowerFromGrid     the configured max charge from grid power
	 * @return the negative set-point or null
	 */
	@Deprecated
	public static int calculateChargeGridPower(Integer essChargePowerInChargeGrid, ManagedSymmetricEss ess,
			int essActivePower, int gridActivePower, int maxChargePowerFromGrid) {
		var realGridPower = gridActivePower + essActivePower; // 'real', without current ESS charge/discharge
		var targetChargePower = essPowerOrElse(essChargePowerInChargeGrid, ess) //
				+ min(0, realGridPower) * -1; // add excess production
		var effectiveGridBuyPower = max(0, realGridPower) + targetChargePower;
		var chargePower = max(0, targetChargePower - max(0, effectiveGridBuyPower - maxChargePowerFromGrid));

		// Invert to negative for CHARGE
		return chargePower * -1;
	}

	@Deprecated
	protected static int essPowerOrElse(Integer power, ManagedSymmetricEss ess) {
		if (power != null) {
			return power;
		}
		var capacity = ess.getCapacity();
		if (capacity.isDefined()) {
			return round(capacity.get() * ESS_CHARGE_C_RATE);
		}
		var maxApparentPower = ess.getMaxApparentPower();
		if (maxApparentPower.isDefined()) {
			return maxApparentPower.get();
		}
		return 0;
	}

	/**
	 * Calculates the ActivePower constraint for
	 * {@link StateMachine#DELAY_DISCHARGE}.
	 * 
	 * @param ess the {@link ManagedSymmetricEss}
	 * @return the set-point
	 */
	@Deprecated
	public static int calculateDelayDischargePower(ManagedSymmetricEss ess) {
		return switch (ess) {
		case HybridEss e ->
			// Limit discharge to DC-PV power
			max(0, ess.getActivePower().orElse(0) - e.getDcDischargePower().orElse(0));
		default ->
			// Limit discharge to 0
			0;
		};
	}

	/**
	 * Post-Process a state during {@link Controller#run()}, i.e. replace with
	 * 'better' state if appropriate.
	 * 
	 * <p>
	 * NOTE: this can be useful, if live operation deviates from predicted
	 * operation, e.g. because predictions were wrong.
	 * 
	 * @param ess               the {@link ManagedSymmetricEss}
	 * @param state             the initial state
	 * @param pwrBalancing      the power set-point as it would be in
	 *                          {@link StateMachine#BALANCING}
	 * @param pwrDelayDischarge the power set-point as it would be in
	 *                          {@link StateMachine#DELAY_DISCHARGE}
	 * @param pwrChargeGrid     the power set-point as it would be in
	 *                          {@link StateMachine#CHARGE_GRID}
	 * @return the new state
	 */
	@Deprecated
	public static StateMachine postprocessRunState(ManagedSymmetricEss ess, StateMachine state, int pwrBalancing,
			int pwrDelayDischarge, int pwrChargeGrid) {
		if (state == CHARGE_GRID) {
			// CHARGE_GRID,...
			if (pwrChargeGrid >= pwrDelayDischarge) {
				// but battery charge/discharge is the same as DELAY_DISCHARGE
				state = DELAY_DISCHARGE;
			}
			var soc = ess.getSoc();
			if (soc.isDefined() && soc.get() >= ESS_MAX_SOC) {
				state = DELAY_DISCHARGE;
			}
		}

		if (state == DELAY_DISCHARGE) {
			// CHARGE_GRID,...
			if (pwrDelayDischarge >= pwrBalancing) {
				// but battery charge/discharge is the same as DELAY_DISCHARGE
				state = BALANCING;
			}
		}

		return state;
	}
}
