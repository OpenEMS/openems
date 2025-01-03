package io.openems.edge.battery.pylontech.powercubem2.statemachine;

import io.openems.edge.battery.pylontech.powercubem2.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class UndefinedHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {

		var battery = context.getParent();

		switch (battery.getStartStopTarget()) {
		case UNDEFINED: {
			// Stuck in undefined state
			return State.UNDEFINED;
		}
		case START: {
			if (battery.hasFaults()) {
				// Faults exist - handle errors
				return State.ERROR;
			} else {
				// No faults, so try to start the battery
				return State.GO_RUNNING;
			}
		}
		case STOP: {
			// If the Start/Stop target state is stop -> then stop it
			return State.GO_STOPPED;
		}
		}

		assert false;
		return State.UNDEFINED; // Should never happen
	}

}