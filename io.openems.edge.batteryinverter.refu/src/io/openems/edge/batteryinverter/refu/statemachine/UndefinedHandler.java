package io.openems.edge.batteryinverter.refu.statemachine;

import io.openems.edge.batteryinverter.refu.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class UndefinedHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		final var inverter = context.getParent();
		return switch (inverter.getStartStopTarget()) {
		case UNDEFINED -> State.UNDEFINED;
		case START -> {
			if (inverter.hasFailure()) {
				yield State.ERROR;
			}
			if (!context.battery.isStarted()) {
				yield State.UNDEFINED;
			}
			yield State.GO_RUNNING;
		}
		case STOP -> State.GO_STOPPED;
		};
	}

}