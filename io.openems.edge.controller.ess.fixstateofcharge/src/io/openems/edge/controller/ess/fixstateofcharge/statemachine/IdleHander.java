package io.openems.edge.controller.ess.fixstateofcharge.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.fixstateofcharge.statemachine.StateMachine.State;

public class IdleHander extends StateHandler<State, Context> {

	@Override
	protected State runAndGetNextState(Context context) throws OpenemsNamedException {

		if (context.config.isRunning()) {
			return State.NOT_STARTED;
		}

		return State.IDLE;
	}
}
