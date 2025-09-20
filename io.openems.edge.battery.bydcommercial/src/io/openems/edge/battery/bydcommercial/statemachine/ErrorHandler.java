package io.openems.edge.battery.bydcommercial.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.bydcommercial.enums.PowerCircuitControl;
import io.openems.edge.battery.bydcommercial.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class ErrorHandler extends StateHandler<State, Context> {

	private Instant entryAt = Instant.MIN;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.entryAt = Instant.now();

		// Try to stop system
		context.getParent()._setPowerCircuitControl(PowerCircuitControl.SWITCH_OFF);
	}

	@Override
	protected void onExit(Context context) throws OpenemsNamedException {
		var battery = context.getParent();

		battery._setMaxStartAttempts(false);
		battery._setMaxStopAttempts(false);
	}

	@Override
	public State runAndGetNextState(Context context) {
		System.out.println("Stuck in ERROR_HANDLING: " + context.getParent().getStateChannel().listStates());

		if (Duration.between(this.entryAt, Instant.now()).getSeconds() > 120) {
			// Try again
			return State.UNDEFINED;
		}

		return State.ERROR;
	}

}
