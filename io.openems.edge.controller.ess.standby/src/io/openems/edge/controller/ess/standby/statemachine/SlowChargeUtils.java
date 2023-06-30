package io.openems.edge.controller.ess.standby.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

public class SlowChargeUtils {

	/**
	 * Charge with at least 'maxApparentPower' x MIN_POWER_LIMIT_FACTOR.
	 */
	private static final float MIN_POWER_LIMIT_FACTOR = 0.2f;

	/**
	 * Charge with at maximum 'maxApparentPower' x MAX_POWER_LIMIT_FACTOR.
	 */
	private static final float MAX_POWER_LIMIT_FACTOR = 0.5f;

	/**
	 * Calculates and applies the charging power for
	 * {@link StateMachine#SLOW_CHARGE_1} and {@link StateMachine#SLOW_CHARGE_2}.
	 *
	 * @param context the Context
	 * @throws IllegalArgumentException on error
	 * @throws OpenemsNamedException    on error
	 */
	protected static void calculateAndApplyPower(Context context)
			throws IllegalArgumentException, OpenemsNamedException {
		// Charge by keeping the grid to zero (just like in Balancing Controller)
		int gridPower = context.sum.getGridActivePower().getOrError();
		int essPower = context.ess.getActivePower().getOrError();
		var setPower = gridPower + essPower;
		if (setPower > 0) {
			// do not discharge
			setPower = 0;
		}

		// Fit within minimum and maximum power limits
		int maxApparentPower = context.ess.getMaxApparentPower().getOrError();
		var minPower = Math.round(maxApparentPower * -1 * MIN_POWER_LIMIT_FACTOR);
		if (setPower > minPower) {
			setPower = minPower;
		}
		var maxPower = Math.round(maxApparentPower * -1 * MAX_POWER_LIMIT_FACTOR);
		if (setPower < maxPower) {
			setPower = maxPower;
		}

		// Apply power constraint
		context.ess.setActivePowerEqualsWithPid(setPower);
	}
}
