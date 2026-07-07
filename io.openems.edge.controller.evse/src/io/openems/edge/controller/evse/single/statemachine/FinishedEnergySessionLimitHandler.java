package io.openems.edge.controller.evse.single.statemachine;

import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.evse.single.statemachine.StateMachine.State;

public class FinishedEnergySessionLimitHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		// Stop charging
		context.applyAdjustedActions(b -> b //
				.setApplyZeroSetPoint());

		return State.FINISHED_ENERGY_SESSION_LIMIT;
	}
}
