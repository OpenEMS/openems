package io.openems.edge.battery.fenecon.home.statemachine;

import io.openems.edge.battery.fenecon.home.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class ErrorHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {

		if (context.getParent().getLowMinVoltage().orElse(false)) {
			return State.GO_STOPPED;
		}

		if (context.getParent().hasFaults()) {
			return State.ERROR;
		} else {
			return State.UNDEFINED;
		}
	}

}
