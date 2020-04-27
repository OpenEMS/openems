package io.openems.edge.battery.bydcommercial.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.bydcommercial.PreChargeControl;
import io.openems.edge.battery.bydcommercial.statemachine.StateMachine.Context;
import io.openems.edge.battery.bydcommercial.utils.Constants;

public class GoStopped extends State.Handler {

	private Instant lastAttempt = Instant.MIN;
	private int attemptCounter = 0;

	@Override
	protected void onEntry(Context context) {
		this.lastAttempt = Instant.MIN;
		this.attemptCounter = 0;
	}

	@Override
	public State getNextState(Context context) throws OpenemsNamedException {
		PreChargeControl preChargeControl = context.component.getPreChargeControl();

		if (preChargeControl == PreChargeControl.SWITCH_OFF) {
			return State.STOPPED;
		}

		boolean isMaxStartTimePassed = Duration.between(this.lastAttempt, Instant.now())
				.getSeconds() > Constants.RETRY_COMMAND_SECONDS;
		if (isMaxStartTimePassed) {
			// First try - or waited long enough for next try

			if (this.attemptCounter > Constants.RETRY_COMMAND_MAX_ATTEMPTS) {
				// Too many tries
				context.component.setMaxStopAttempts(true);
				return State.UNDEFINED;

			} else {
				// Trying to switch off
				context.component.setPreChargeControl(PreChargeControl.SWITCH_OFF);
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
