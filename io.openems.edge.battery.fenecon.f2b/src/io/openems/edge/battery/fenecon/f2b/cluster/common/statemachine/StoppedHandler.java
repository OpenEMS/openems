package io.openems.edge.battery.fenecon.f2b.cluster.common.statemachine;

import io.openems.edge.battery.fenecon.f2b.cluster.common.statemachine.StateMachine.State;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;

public class StoppedHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		final var cluster = context.getParent();

		// Has Faults -> error handling
		if (cluster.hasFaults()) {
			return State.ERROR;
		}

		if (cluster.hasBatteriesFault()) {
			cluster._setAtLeastOneBatteryInError(true);
			return State.ERROR;
		}

		// Check if all batteries are stopped
		if (!cluster.areAllBatteriesStopped()) {
			cluster._setAtLeastOneBatteryNotStopped(true);
			return State.ERROR;
		}

		if (cluster.getStartStopTarget() == StartStop.START) {
			return State.GO_RUNNING;
		}

		// Mark as stopped
		cluster._setStartStop(StartStop.STOP);
		cluster._setAtLeastOneBatteryNotStopped(false);
		return State.STOPPED;
	}
}
