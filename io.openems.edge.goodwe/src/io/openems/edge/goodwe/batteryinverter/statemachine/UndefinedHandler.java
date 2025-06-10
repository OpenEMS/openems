package io.openems.edge.goodwe.batteryinverter.statemachine;

import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.goodwe.batteryinverter.statemachine.StateMachine.State;

public class UndefinedHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		var inverter = context.getParent();
		return switch (inverter.getStartStopTarget()) {
		case UNDEFINED -> State.UNDEFINED;
		case START -> State.GO_RUNNING;
		case STOP -> State.GO_STOPPED;
		};
	}
}
