package io.openems.edge.controller.ess.standby.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.standby.statemachine.StateMachine.State;

public class SlowCharge1Handler extends StateHandler<State, Context> {

	/**
	 * Switch to next {@link State#FAST_CHARGE} after MAX_STATE_DURATION_MINUTES.
	 */
	private static final int MAX_STATE_DURATION_MINUTES = 30;

	private Instant enteredStateAt = null;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.enteredStateAt = Instant.now(context.clock);
	}

	@Override
	public State runAndGetNextState(Context context) throws IllegalArgumentException, OpenemsNamedException {
		SlowChargeUtils.calculateAndApplyPower(context);

		// Evaluate next state
		if (Duration.between(this.enteredStateAt, Instant.now(context.clock))
				.toMinutes() >= MAX_STATE_DURATION_MINUTES) {
			// time passed
			return State.FAST_CHARGE;
		}
		// stay in this State
		return State.SLOW_CHARGE_1;
	}

}
