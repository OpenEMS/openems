package io.openems.edge.battery.fenecon.home.statemachine;

import io.openems.edge.battery.fenecon.home.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class UndefinedHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		var battery = context.getParent();

		return switch (battery.getStartStopTarget()) {
		case UNDEFINED ->
			// Stuck in UNDEFINED State
			State.UNDEFINED;

		case START -> {
			// force START
			if (battery.getModbusCommunicationFailed()) {
				// Modbus Communication Failed -> try to start
				yield State.GO_RUNNING;
			} else if (battery.hasFaults()) {
				// Has Faults -> error handling
				yield State.ERROR;
			} else {
				// No Faults -> start
				yield State.GO_RUNNING;
			}
		}

		case STOP ->
			// STOP is impossible -> stuck in GO_STOPPED State
			State.GO_STOPPED;
		};
	}

}
