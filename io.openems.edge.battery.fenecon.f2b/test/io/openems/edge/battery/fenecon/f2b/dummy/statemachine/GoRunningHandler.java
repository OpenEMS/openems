package io.openems.edge.battery.fenecon.f2b.dummy.statemachine;

import io.openems.edge.battery.fenecon.f2b.dummy.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class GoRunningHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		final var battery = context.getParent();
		if (battery.getHvContactorUnlocked()) {
			return State.RUNNING;
		}
		return State.GO_RUNNING;
	}
}
