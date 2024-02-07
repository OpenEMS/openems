package io.openems.edge.system.fenecon.industrial.s.statemachine;

import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.system.fenecon.industrial.s.statemachine.StateMachine.State;

public class StoppedHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		final var industrialS = context.getParent();
		final var ess = context.ess;

		if (ess.hasFaults() || industrialS.hasFaults()) {
			return State.ERROR;
		}

		if (industrialS.getStartStopTarget() == StartStop.START) {
			return State.GO_RUNNING;
		}

		return State.STOPPED;
	}
}
