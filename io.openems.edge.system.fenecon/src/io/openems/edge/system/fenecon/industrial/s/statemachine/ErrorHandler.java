package io.openems.edge.system.fenecon.industrial.s.statemachine;

import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.system.fenecon.industrial.s.statemachine.StateMachine.State;

public class ErrorHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		final var ess = context.ess;
		if (!ess.hasFaults()) {
			return State.GO_STOPPED;
		}
		return State.ERROR;
	}
}
