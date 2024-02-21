package io.openems.edge.system.fenecon.industrial.s.statemachine;

import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.system.fenecon.industrial.s.statemachine.StateMachine.State;

public class RunningHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		final var industrialS = context.getParent();
		final var ess = context.ess;

		if (ess.hasFaults()//
				|| industrialS.hasFaults() //
				|| (ess instanceof StartStoppable e && !e.isStarted())//
		) {
			return State.ERROR;
		}

		// If StartStopTarget is Start and Emergency Stop not triggered, start the ESS
		if (industrialS.getStartStopTarget() == StartStop.STOP) {
			return State.GO_STOPPED;
		}

		return State.RUNNING;
	}
}
