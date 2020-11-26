package io.openems.edge.battery.fenecon.home.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.fenecon.home.statemachine.StateMachine.State;
import io.openems.edge.battery.poweramp.enums.BMSControl;
import io.openems.edge.battery.poweramp.utils.Constants;
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
		BMSControl bmsControl = context.component.getBMSControl();

		if (bmsControl == BMSControl.SWITCHED_OFF) {
			return State.STOPPED;
		}

		boolean isMaxStartTimePassed = Duration.between(this.lastAttempt, Instant.now())
				.getSeconds() > Constants.RETRY_COMMAND_SECONDS;
		if (isMaxStartTimePassed) {
			// First try - or waited long enough for next try

			if (this.attemptCounter > Constants.RETRY_COMMAND_MAX_ATTEMPTS) {
				// Too many tries
				context.component._setMaxStopAttempts(true);
				return State.UNDEFINED;

			} else {
				// Trying to switch off
				context.component.setBMSControl(BMSControl.SWITCHED_OFF);
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
