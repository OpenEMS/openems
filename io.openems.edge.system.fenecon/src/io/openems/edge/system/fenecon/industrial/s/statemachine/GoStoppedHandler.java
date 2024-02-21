package io.openems.edge.system.fenecon.industrial.s.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.system.fenecon.industrial.s.statemachine.StateMachine.State;

public class GoStoppedHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		final var industrialS = context.getParent();
		final var ess = context.ess;
		if (ess.hasFaults() || industrialS.hasFaults()) {
			return State.ERROR;
		}

		if (ess instanceof StartStoppable e) {
			e.stop();
			if (e.isStopped()) {
				return State.STOPPED;
			}
		}
		return State.GO_STOPPED;
	}
}
