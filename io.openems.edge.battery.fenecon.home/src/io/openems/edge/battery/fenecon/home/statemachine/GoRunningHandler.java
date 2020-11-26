package io.openems.edge.battery.fenecon.home.statemachine;


import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.fenecon.home.statemachine.StateMachine.State;
import io.openems.edge.battery.poweramp.enums.BMSControl;
import io.openems.edge.battery.poweramp.utils.Constants;
import io.openems.edge.common.statemachine.StateHandler;

public class GoRunningHandler extends StateHandler<State, Context> {

	private Instant lastAttempt = Instant.MIN;
	private int attemptCounter = 0;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.lastAttempt = Instant.MIN;
		this.attemptCounter = 0;
		context.component._setMaxStartAttempts(false);
	}

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		BMSControl bMSControl = context.component.getBMSControl();

		if (bMSControl == BMSControl.SWITCHED_ON) {
			return State.RUNNING;
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
//				context.component.setBMSControl(BMSControl.SWITCH_ON);
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
