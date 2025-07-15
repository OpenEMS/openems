package io.openems.edge.controller.evse.single.statemachine;

import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.evse.single.statemachine.StateMachine.State;

public class ChargingHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		final var phaseSwitch = context.actions.phaseSwitch();
		if (phaseSwitch != null) {
			return switch (phaseSwitch) {
			case TO_SINGLE_PHASE -> State.PHASE_SWITCH_TO_SINGLE_PHASE;
			case TO_THREE_PHASE -> State.PHASE_SWITCH_TO_THREE_PHASE;
			};
		}

		// Apply Actions directly
		context.apply(context.actions);
		return State.CHARGING;
	}
}
