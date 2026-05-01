package io.openems.edge.heat.askoma.statemachine;

import static io.openems.edge.heat.askoma.statemachine.AskomaConstants.OFF_ACTIVE_POWER;

import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.heat.askoma.statemachine.StateMachine.State;

public class OffHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		context.resetFastHeatState();
		context.setTargetActivePowerForHeatElement(OFF_ACTIVE_POWER);
		return State.OFF;
	}
}
