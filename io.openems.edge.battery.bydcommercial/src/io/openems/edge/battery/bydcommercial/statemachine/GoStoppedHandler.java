package io.openems.edge.battery.bydcommercial.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.bydcommercial.enums.PowerCircuitControl;
import io.openems.edge.battery.bydcommercial.statemachine.StateMachine.State;
import io.openems.edge.battery.bydcommercial.utils.Constants;
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
		var powerCircuitControl = battery.getPowerCircuitControl();

		if (powerCircuitControl == PowerCircuitControl.SWITCH_OFF) {
			return State.STOPPED;
		}

		var isMaxStartTimePassed = Duration.between(this.lastAttempt, Instant.now())
				.getSeconds() > Constants.RETRY_COMMAND_SECONDS;
		if (!isMaxStartTimePassed) {
			// Still waiting...
			return State.GO_STOPPED;
		}

		if (this.attemptCounter > Constants.RETRY_COMMAND_MAX_ATTEMPTS) {
			// Too many tries
			battery._setMaxStopAttempts(true);
			return State.UNDEFINED;

		}

		// Trying to switch off
		battery.setPowerCircuitControl(PowerCircuitControl.SWITCH_OFF);
		this.lastAttempt = Instant.now();
		this.attemptCounter++;
		return State.GO_STOPPED;
	}

}
