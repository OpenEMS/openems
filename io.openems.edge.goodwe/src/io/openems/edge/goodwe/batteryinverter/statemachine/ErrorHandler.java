package io.openems.edge.goodwe.batteryinverter.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.goodwe.batteryinverter.statemachine.StateMachine.State;

public class ErrorHandler extends StateHandler<State, Context> {

	private static final int WAIT_IN_ERROR_STATE_SECONDS = 120;

	private Instant entryAt = Instant.MIN;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.entryAt = Instant.now(context.clock);
	}

	@Override
	public State runAndGetNextState(Context context) {
		var now = Instant.now(context.clock);
		if (Duration.between(this.entryAt, now).getSeconds() > WAIT_IN_ERROR_STATE_SECONDS) {
			// Try again
			return State.UNDEFINED;
		}

		return State.ERROR;
	}

}
