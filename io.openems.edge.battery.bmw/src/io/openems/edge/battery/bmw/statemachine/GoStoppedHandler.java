package io.openems.edge.battery.bmw.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.bmw.statemachine.StateMachine.State;
import io.openems.edge.battery.bmw.utils.Constants;
import io.openems.edge.common.statemachine.StateHandler;

public class GoStoppedHandler extends StateHandler<State, Context> {

	private Instant lastAttempt = Instant.MIN;
	private int attemptCounter = 0;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.lastAttempt = Instant.MIN;
		this.attemptCounter = 0;
		context.component._setMaxStartAttempts(false);
	}

	@Override
	protected State runAndGetNextState(Context context) throws OpenemsNamedException {

		if (context.component.isSystemStopped()) {
			return State.STOPPED;
		}

		boolean isMaxStartTimePassed = Duration.between(this.lastAttempt, Instant.now())
				.getSeconds() > Constants.RETRY_COMMAND_SECONDS;
		if (isMaxStartTimePassed) {
			// First try - or waited long enough for next try

			if (this.attemptCounter > Constants.RETRY_COMMAND_MAX_ATTEMPTS) {
				// Too many tries
				context.component._setMaxStartAttempts(true);
				return State.UNDEFINED;

			} else {
				// Trying to switch on
				context.component._setBmwStartStop(Constants.OPEN_CONTACTORS);

				this.lastAttempt = Instant.now();
				this.attemptCounter++;
				return State.GO_STOPPED;

			}

		} else {
			// Still waiting...
			return State.GO_STOPPED;
		}

	}
}
