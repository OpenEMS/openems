package io.openems.edge.system.fenecon.industrial.s.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.system.fenecon.industrial.s.statemachine.StateMachine.State;

public class GoRunningHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		final var industrialS = context.getParent();
		final var ess = context.ess;
		if (ess.hasFaults() || industrialS.hasFaults()) {
			return State.ERROR;
		}

		if (industrialS.getStartStopTarget() == StartStop.STOP) {
			return State.GO_STOPPED;
		}

		if (!industrialS.isInEmergencyStopState() && ess instanceof StartStoppable e) {
			e.start();
			if (e.isStarted()) {
				return State.RUNNING;
			}
		}
		return State.GO_RUNNING;
	}
}
