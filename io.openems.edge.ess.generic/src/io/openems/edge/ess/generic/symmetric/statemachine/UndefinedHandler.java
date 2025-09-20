package io.openems.edge.ess.generic.symmetric.statemachine;

import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.ess.generic.symmetric.statemachine.StateMachine.State;

public class UndefinedHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		final var ess = context.getParent();
		return switch (ess.getStartStopTarget()) {
		case UNDEFINED -> State.UNDEFINED;
		case START -> {
			if (ess.hasFaults() || context.batteryInverter.hasFaults()) {
				yield State.ERROR;
			}
			yield State.START_BATTERY;
		}
		case STOP -> State.STOP_BATTERY_INVERTER;
		};
	}

}
