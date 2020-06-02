package io.openems.edge.controller.ess.standby.statemachine;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import io.openems.common.exceptions.InvalidValueException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;

public class SlowCharge1 extends StateHandler<State, Context> {

	/*
	 * Charge with at least 'maxApparentPower' x MIN_POWER_LIMIT_FACTOR.
	 */
	private static final float MIN_POWER_LIMIT_FACTOR = 0.3f;

	/*
	 * Charge with at maximum 'maxApparentPower' x MAX_POWER_LIMIT_FACTOR.
	 */
	private static final float MAX_POWER_LIMIT_FACTOR = 0.5f;

	@Override
	public State getNextState(Context context) throws IllegalArgumentException, OpenemsNamedException {
		// Charge by keeping the grid to zero (just like in Balancing Controller)
		int gridPower = context.sum.getGridActivePower().value().getOrError();
		int essPower = context.ess.getActivePower().value().getOrError();
		int setPower = gridPower + essPower;

		int maxApparentPower = context.ess.getMaxApparentPower().value().getOrError();
		if()
		
		// Apply power limit
		setPower = this.dischargePowerLimitHandler.applyPowerLimit(context, setPower);

		// Apply power constraint
		context.ess.getSetActivePowerEqualsWithPid().setNextWriteValue(setPower);

		// Evaluate next state
		return this.evaluateNextStateHandler.getNextState(context);
	}

}
