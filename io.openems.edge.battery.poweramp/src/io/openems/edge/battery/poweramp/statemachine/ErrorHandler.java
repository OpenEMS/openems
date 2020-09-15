package io.openems.edge.battery.poweramp.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.poweramp.enums.BMSControl;
import io.openems.edge.battery.poweramp.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class ErrorHandler extends StateHandler<State, Context> {

	private Instant entryAt = Instant.MIN;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.entryAt = Instant.now();

		// Try to stop system
		context.component.setBMSControl(BMSControl.SWITCH_OFF);
	}

	@Override
	protected void onExit(Context context) throws OpenemsNamedException {
		context.component._setMaxStartAttempts(false);
		context.component._setMaxStopAttempts(false);
	}

	@Override
	public State runAndGetNextState(Context context) {
		System.out.println("Stuck in ERROR_HANDLING: " + context.component.getStateChannel().listStates());

		if (Duration.between(this.entryAt, Instant.now()).getSeconds() > 120) {
			// Try again
			return State.UNDEFINED;
		}

		return State.ERROR;
	}

}
