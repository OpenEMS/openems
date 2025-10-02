package io.openems.edge.controller.evse.single.statemachine;

import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.evse.single.statemachine.StateMachine.State;

public class EvNotConnectedHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		// Allow charge with minimum power
		context.applyMinSetPointActions();

		if (context.actions.abilities().isEvConnected()) {
			return State.EV_CONNECTED;
		}

		return State.EV_NOT_CONNECTED;
	}
}
