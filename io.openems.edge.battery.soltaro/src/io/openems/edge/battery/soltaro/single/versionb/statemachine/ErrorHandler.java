package io.openems.edge.battery.soltaro.single.versionb.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.soltaro.single.versionb.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class ErrorHandler extends StateHandler<State, Context> {

	private Instant entryAt = Instant.MIN;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.entryAt = Instant.now();

		ControlAndLogic.stopSystem(context.getParent());
	}

	@Override
	protected void onExit(Context context) throws OpenemsNamedException {
		var battery = context.getParent();
		battery._setMaxStartAttempts(false);
		battery._setMaxStopAttempts(false);
	}

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		if (Duration.between(this.entryAt, Instant.now()).getSeconds() > context.config.errorLevel2Delay()) {
			ControlAndLogic.resetSystem(context.getParent());
			ControlAndLogic.sleepSystem(context.getParent());
			// Try again
			return State.UNDEFINED;
		}

		return State.ERROR;
	}

}
