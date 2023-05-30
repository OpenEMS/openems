package io.openems.edge.battery.statemachine;

import io.openems.edge.battery.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class UndefinedHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		var battery = context.getParent();
		return switch (battery.getStartStopTarget()) {
		case UNDEFINED -> State.UNDEFINED;
		case START -> {
			if (battery.hasFaults()) {
				yield State.ERROR;
			} else {
				yield State.GO_RUNNING;
			}
		}
		case STOP -> State.GO_STOPPED;
		};
	}
}
