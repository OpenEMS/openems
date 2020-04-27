package io.openems.edge.battery.bydcommercial.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.bydcommercial.PreChargeControl;
import io.openems.edge.battery.bydcommercial.statemachine.StateMachine.Context;
import io.openems.edge.battery.bydcommercial.utils.Constants;

public class GoRunning extends State.Handler {

	private Instant lastAttempt = Instant.MIN;
	private int attemptCounter = 0;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.lastAttempt = Instant.MIN;
		this.attemptCounter = 0;
		context.component.setMaxStartAttempts(false);
	}

	@Override
	public State getNextState(Context context) throws OpenemsNamedException {
		PreChargeControl preChargeControl = context.component.getPreChargeControl();

		if (preChargeControl == PreChargeControl.RUNNING) {
			return State.RUNNING;
		}

		boolean isMaxStartTimePassed = Duration.between(this.lastAttempt, Instant.now())
				.getSeconds() > Constants.RETRY_COMMAND_SECONDS;
		if (isMaxStartTimePassed) {
			// First try - or waited long enough for next try

			if (this.attemptCounter > Constants.RETRY_COMMAND_MAX_ATTEMPTS) {
				// Too many tries
				context.component.setMaxStartAttempts(true);
				return State.UNDEFINED;

			} else {
				// Trying to switch on
				context.component.setPreChargeControl(PreChargeControl.SWITCH_ON);
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
