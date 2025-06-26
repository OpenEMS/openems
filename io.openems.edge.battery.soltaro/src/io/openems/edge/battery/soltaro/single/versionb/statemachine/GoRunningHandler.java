package io.openems.edge.battery.soltaro.single.versionb.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.soltaro.single.versionb.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class GoRunningHandler extends StateHandler<State, Context> {

	private Instant lastAttempt = Instant.MIN;
	private int attemptCounter = 0;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.lastAttempt = Instant.MIN;
		this.attemptCounter = 0;
		var battery = context.getParent();
		battery._setMaxStartAttempts(false);
	}

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		var battery = context.getParent();

		if (ControlAndLogic.isSystemRunning(battery)) {
			return State.RUNNING;
		}

		var isMaxStartTimePassed = Duration.between(this.lastAttempt, Instant.now())
				.getSeconds() > ControlAndLogic.RETRY_COMMAND_SECONDS;
		if (isMaxStartTimePassed) {
			// First try - or waited long enough for next try

			if (this.attemptCounter > ControlAndLogic.RETRY_COMMAND_MAX_ATTEMPTS) {
				// Too many tries
				battery._setMaxStartAttempts(true);
				return State.UNDEFINED;

			} else {
				// Trying to switch on
				ControlAndLogic.startSystem(battery);
				this.lastAttempt = Instant.now();
				this.attemptCounter++;
				return State.GO_RUNNING;

			}

		} else {
			// Still waiting...
			return State.GO_RUNNING;
		}
	}

}
