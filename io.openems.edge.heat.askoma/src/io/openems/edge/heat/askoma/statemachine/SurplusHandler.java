package io.openems.edge.heat.askoma.statemachine;

import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.heat.askoma.statemachine.StateMachine.State;

public class SurplusHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		context.resetFastHeatState();
		context.setTargetActivePowerForHeatElement(context.getGridActivePower());
		return State.SURPLUS;
	}
}
