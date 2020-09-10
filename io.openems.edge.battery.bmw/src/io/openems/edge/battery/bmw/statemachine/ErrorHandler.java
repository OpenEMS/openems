package io.openems.edge.battery.bmw.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.bmw.statemachine.StateMachine.State;
import io.openems.edge.battery.bmw.utils.Constants;
import io.openems.edge.common.statemachine.StateHandler;

public class ErrorHandler extends StateHandler<State, Context> {

	private Instant entryAt = Instant.MIN;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.entryAt = Instant.now();

		// Try to stop system
		context.component._setBmwStartStop(Constants.OPEN_CONTACTORS);
	}

	@Override
	protected void onExit(Context context) throws OpenemsNamedException {
		context.component._setMaxStartAttempts(false);
		context.component._setMaxStopAttempts(false);
	}

	@Override
	public State runAndGetNextState(Context context) {
		System.out.println("Stuck in ERROR_HANDLING: " + context.component.getStateChannel().listStates());
		// Try to clear the Error
		context.component.clearError();

		if (Duration.between(this.entryAt, Instant.now()).getSeconds() > Constants.ERROR_DELAY) {
			// Try again
			return State.UNDEFINED;
		}

		return State.ERROR;
	}
}
