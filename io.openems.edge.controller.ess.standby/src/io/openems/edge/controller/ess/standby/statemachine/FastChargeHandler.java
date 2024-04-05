package io.openems.edge.controller.ess.standby.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.standby.statemachine.StateMachine.State;

/**
 * Charge the battery with maximum power for 10 minutes.
 */
public class FastChargeHandler extends StateHandler<State, Context> {

	/**
	 * Switch to next {@link State#SLOW_CHARGE_2} after MAX_STATE_DURATION_MINUTES.
	 */
	private static final int MAX_STATE_DURATION_MINUTES = 10;

	private Instant enteredStateAt = null;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.enteredStateAt = Instant.now(context.clock);
	}

	@Override
	public State runAndGetNextState(Context context) throws IllegalArgumentException, OpenemsNamedException {
		int maxApparentPower = context.ess.getMaxApparentPower().getOrError();
		var setPower = maxApparentPower * -1; // Charge

		// Apply power constraint
		context.ess.setActivePowerEqualsWithPid(setPower);

		// Evaluate next state
		if (Duration.between(this.enteredStateAt, Instant.now(context.clock))
				.toMinutes() >= MAX_STATE_DURATION_MINUTES) {
			// time passed
			return State.SLOW_CHARGE_2;
		}
		// stay in this State
		return State.FAST_CHARGE;
	}

}
