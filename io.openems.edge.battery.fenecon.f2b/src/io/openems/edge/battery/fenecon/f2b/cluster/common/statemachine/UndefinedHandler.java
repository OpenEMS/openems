package io.openems.edge.battery.fenecon.f2b.cluster.common.statemachine;

import io.openems.edge.battery.fenecon.f2b.cluster.common.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class UndefinedHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		final var cluster = context.getParent();

		if (cluster.hasBatteriesFault()) {
			return State.ERROR;
		}

		if (cluster.areAllBatteriesStarted()) {
			return State.RUNNING;
		}

		if (cluster.areAllBatteriesStopped()) {
			return State.STOPPED;
		}

		return State.UNDEFINED;
	}
}
