package io.openems.edge.goodwe.batteryinverter.statemachine;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.goodwe.batteryinverter.statemachine.StateMachine.State;

public class GoStoppedHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) throws OpenemsException {
		return State.STOPPED;
	}
}
