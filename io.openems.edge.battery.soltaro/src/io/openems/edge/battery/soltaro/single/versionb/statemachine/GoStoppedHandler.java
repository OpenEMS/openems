package io.openems.edge.battery.soltaro.single.versionb.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.soltaro.single.versionb.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class GoStoppedHandler extends StateHandler<State, Context> {

	private Instant lastAttempt = Instant.MIN;
	private int attemptCounter = 0;

	@Override
	protected void onEntry(Context context) {
		this.lastAttempt = Instant.MIN;
		this.attemptCounter = 0;
	}

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		var battery = context.getParent();

		if (ControlAndLogic.isSystemStopped(battery)) {
			return State.STOPPED;
		}

		var isMaxStartTimePassed = Duration.between(this.lastAttempt, Instant.now())
				.getSeconds() > ControlAndLogic.RETRY_COMMAND_SECONDS;
		if (isMaxStartTimePassed) {
			// First try - or waited long enough for next try

			if (this.attemptCounter > ControlAndLogic.RETRY_COMMAND_MAX_ATTEMPTS) {
				// Too many tries
				battery._setMaxStopAttempts(true);
				return State.UNDEFINED;

			} else {
				// Trying to switch off
				ControlAndLogic.stopSystem(battery);
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
