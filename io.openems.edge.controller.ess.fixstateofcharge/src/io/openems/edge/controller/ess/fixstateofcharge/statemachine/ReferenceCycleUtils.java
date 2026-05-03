package io.openems.edge.controller.ess.fixstateofcharge.statemachine;

import io.openems.common.exceptions.InvalidValueException;
import io.openems.edge.controller.ess.fixstateofcharge.api.AbstractFixStateOfCharge;

/**
 * Utility methods for reference cycle calculations shared across state
 * handlers.
 */
public final class ReferenceCycleUtils {

	/** Reference cycle power rate: 0.5C (50% of capacity). */
	public static final float REFERENCE_POWER_C_RATE = 0.5f;

	/** Reference cycle pause duration in milliseconds (30 minutes). */
	public static final long REFERENCE_CYCLE_PAUSE_MS = 30 * 60 * 1000L;

	private ReferenceCycleUtils() {
		// Utility class - prevent instantiation
	}

	/**
	 * Calculates the maximum reference power based on battery capacity.
	 *
	 * <p>
	 * Uses 50% of capacity (0.5C rate) as reference power, limited by max apparent
	 * power. Falls back to max apparent power if capacity is unavailable or
	 * invalid.
	 *
	 * @param maxApparentPower the maximum apparent power available in watts
	 * @param capacityWh       the ESS capacity in Wh (may be null or ≤ 0)
	 * @return the maximum reference power in watts
	 */
	public static int calculateMaxReferencePower(int maxApparentPower, Integer capacityWh) {
		if (capacityWh == null || capacityWh <= 0) {
			return maxApparentPower;
		}
		return Math.min(maxApparentPower, Math.round(capacityWh * REFERENCE_POWER_C_RATE));
	}

	/**
	 * Calculate required time including reference cycle phases from a state-machine
	 * context.
	 *
	 * @param context state-machine context
	 * @return total required time in seconds for the complete reference cycle
	 */
	public static long calculateRequiredTimeWithReferenceCycle(Context context) throws InvalidValueException {
		var capacity = context.getEssCapacityForEstimationWh();
		var powerToTargetW = context.getTimeEstimationPowerW(capacity);
		return calculateRequiredTimeWithReferenceCycle(context, capacity, powerToTargetW);
	}

	private static long calculateRequiredTimeWithReferenceCycle(Context context, int capacity, int powerToTargetW) {
		// Determine reference target based on current SoC
		var refTarget = context.soc >= 70 ? 100 : 0;

		// Calculate reference power (clamped by max apparent power)
		var refPowerW = calculateMaxReferencePower(context.maxApparentPower, capacity);

		// Calculate time segments
		var secondsToRef = AbstractFixStateOfCharge.calculateRequiredTime(context.soc, refTarget, capacity, refPowerW,
				context.clock);
		var pauseSeconds = REFERENCE_CYCLE_PAUSE_MS / 1000;
		var secondsRefToTarget = AbstractFixStateOfCharge.calculateRequiredTime(refTarget, context.targetSoc, capacity,
				powerToTargetW, context.clock);

		return secondsToRef + pauseSeconds + secondsRefToTarget;
	}
}
