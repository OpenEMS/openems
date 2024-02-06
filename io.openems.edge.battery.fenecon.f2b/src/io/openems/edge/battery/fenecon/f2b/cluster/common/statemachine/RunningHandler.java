package io.openems.edge.battery.fenecon.f2b.cluster.common.statemachine;

import io.openems.edge.battery.fenecon.f2b.cluster.common.statemachine.StateMachine.State;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;

public class RunningHandler extends StateHandler<State, Context> {

	@Override
	protected void onEntry(Context context) {
		final var cluster = context.getParent();
		cluster._setOneBatteryStopped(cluster.isOneBatteryStartedAndOneStopped());
		cluster._setOneBatteryHasError(cluster.getAndUpdateHasAnyBatteryFault());
	}

	@Override
	public State runAndGetNextState(Context context) {
		final var cluster = context.getParent();

		// Has Faults -> error handling
		if (cluster.getAndUpdateHasAllBatteriesFault() || cluster.hasFaults()) {
			return State.ERROR;
		}

		if (cluster.getAndUpdateHasAnyBatteryFault() && cluster.isSerialCluster()) {
			return State.ERROR;
		}

		if (!cluster.areAllBatteriesStarted()) {
			cluster._setOneBatteryNotRunning(true);
			return State.ERROR;
		}

		if (cluster.getStartStopTarget() == StartStop.STOP) {
			return State.GO_STOPPED;
		}

		// Mark as started
		cluster._setStartStop(StartStop.START);
		return State.RUNNING;
	}
}
