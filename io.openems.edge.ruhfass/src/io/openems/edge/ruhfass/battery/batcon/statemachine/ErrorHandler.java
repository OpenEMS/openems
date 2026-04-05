package io.openems.edge.ruhfass.battery.batcon.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.ruhfass.battery.batcon.statemachine.StateMachine.State;

public class ErrorHandler extends StateHandler<State, Context> {

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		var battery = context.getParent();

		// Set Max allowed Currents 0!
		battery._setChargeMaxCurrent(0);
		battery._setDischargeMaxCurrent(0);
	}

	@Override
	protected void onExit(Context context) throws OpenemsNamedException {
		var battery = context.getParent();
		battery._setMaxStartTime(false);
		battery._setMaxStopTime(false);
	}

	@Override
	public State runAndGetNextState(Context context) throws IllegalArgumentException, OpenemsNamedException {
		var battery = context.getParent();

		if (!battery.hasFaults()) {
			switch (battery.getStartStopTarget()) {
			case UNDEFINED:
				return State.TARGET_UNDEFINED;
			case START:
				return State.GO_RUNNING;
			case STOP:
				return State.GO_STOPPED;
			}
		}
		return State.ERROR;
	}

}
