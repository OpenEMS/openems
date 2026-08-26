package io.openems.edge.battery.bmw.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.bmw.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class ErrorHandler extends StateHandler<State, Context> {

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		final var battery = context.getParent();
		battery.stop();
	}

	@Override
	public State runAndGetNextState(Context context) {
		final var battery = context.getParent();
		if (!battery.hasFaults() && battery.isShutdown()) {
			return State.STOPPED;
		}
		return State.ERROR;
	}

	@Override
	protected void onExit(Context context) {
		final var battery = context.getParent();
		battery._setUnexpectedRunningState(false);
		battery._setUnexpectedStoppedState(false);
		battery._setTimeoutStartBattery(false);
		battery._setTimeoutStopBattery(false);
	}
}
