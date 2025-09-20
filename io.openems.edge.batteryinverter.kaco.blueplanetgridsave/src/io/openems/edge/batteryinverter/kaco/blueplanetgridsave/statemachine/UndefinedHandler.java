package io.openems.edge.batteryinverter.kaco.blueplanetgridsave.statemachine;

import io.openems.edge.batteryinverter.kaco.blueplanetgridsave.statemachine.StateMachine.State;
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
			yield State.GO_RUNNING;
		}
		case STOP -> State.GO_STOPPED;
		};
	}

}
