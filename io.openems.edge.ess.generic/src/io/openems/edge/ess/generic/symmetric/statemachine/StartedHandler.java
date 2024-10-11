package io.openems.edge.ess.generic.symmetric.statemachine;

import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.ess.generic.symmetric.statemachine.StateMachine.State;

public class StartedHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		final var ess = context.getParent();

		if (context.hasFaults()) {
			return State.ERROR;
		}

		if (!context.isStarted()) {
			return State.ERROR;
		}

		// Mark as started
		ess._setStartStop(StartStop.START);
		return State.STARTED;
	}
}
