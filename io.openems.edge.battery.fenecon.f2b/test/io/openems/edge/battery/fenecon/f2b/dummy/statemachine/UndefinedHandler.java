package io.openems.edge.battery.fenecon.f2b.dummy.statemachine;

import io.openems.edge.battery.fenecon.f2b.dummy.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class UndefinedHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		final var battery = context.getParent();

		if (battery.hasFaults()) {
			return State.ERROR;
		}

		if (battery.isStarted()) {
			return State.RUNNING;
		}

		if (battery.isStopped()) {
			return State.STOPPED;
		}

		return State.GO_STOPPED;
	}
}
