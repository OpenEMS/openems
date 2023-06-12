package io.openems.edge.battery.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class ErrorHandler extends StateHandler<State, Context> {

	private static final int SECONDS_UNTIL_RETRY = 120;
	private Instant entryAt = Instant.MIN;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.entryAt = Instant.now(context.componentManager.getClock());
	}

	@Override
	protected void onExit(Context context) throws OpenemsNamedException {
		final var battery = context.getParent();
		battery._setMaxStartAttempts(false);
		battery._setMaxStopAttempts(false);
	}

	@Override
	public State runAndGetNextState(Context context) {
		if (Duration.between(this.entryAt, Instant.now(context.componentManager.getClock()))
				.getSeconds() > SECONDS_UNTIL_RETRY) {
			return State.UNDEFINED;
		}
		return State.ERROR;
	}
}
