package io.openems.edge.ess.generic.symmetric.statemachine;

import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.ess.generic.symmetric.statemachine.StateMachine.State;

public class StoppedHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		final var ess = context.getParent();

		if (context.hasFaults()) {
			return State.ERROR;
		}

		if (!context.isStopped()) {
			return State.ERROR;
		}

		ess._setStartStop(StartStop.STOP);
		return State.STOPPED;
	}

}
