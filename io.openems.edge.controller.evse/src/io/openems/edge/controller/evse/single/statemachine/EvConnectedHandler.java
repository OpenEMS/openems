package io.openems.edge.controller.evse.single.statemachine;

import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.evse.single.statemachine.StateMachine.State;

public class EvConnectedHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		// Allow charge with minimum power
		context.applyMinSetPointActions();

		// TODO Initiate Charging; handle EV_CONNECTED_AWAITING_... states

		return State.CHARGING;
	}
}
