package io.openems.edge.battery.bydcommercial.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.bydcommercial.enums.PowerCircuitControl;
import io.openems.edge.battery.bydcommercial.statemachine.StateMachine.State;
import io.openems.edge.battery.bydcommercial.utils.Constants;
import io.openems.edge.common.statemachine.StateHandler;

public class GoRunningHandler extends StateHandler<State, Context> {

	private Instant lastAttempt = Instant.MIN;
	private int attemptCounter = 0;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.lastAttempt = Instant.MIN;
		this.attemptCounter = 0;
		context.getParent()._setMaxStartAttempts(false);
	}

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		var battery = context.getParent();
		var preChargeControl = battery.getPowerCircuitControl();

		if (preChargeControl == PowerCircuitControl.SWITCH_ON) {
			return State.RUNNING;
		}

		var isMaxStartTimePassed = Duration.between(this.lastAttempt, Instant.now())
				.getSeconds() > Constants.RETRY_COMMAND_SECONDS;
		if (!isMaxStartTimePassed) {
			// Still waiting...
			return State.GO_RUNNING;
		}

		if (this.attemptCounter > Constants.RETRY_COMMAND_MAX_ATTEMPTS) {
			// Too many tries
			battery._setMaxStartAttempts(true);
			return State.UNDEFINED;
		}

		// Trying to switch on
		battery.setPowerCircuitControl(PowerCircuitControl.PRE_CHARGING_1);
		this.lastAttempt = Instant.now();
		this.attemptCounter++;
		return State.GO_RUNNING;
	}

}
