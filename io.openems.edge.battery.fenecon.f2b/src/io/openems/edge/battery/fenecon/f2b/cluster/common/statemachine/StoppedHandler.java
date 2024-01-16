package io.openems.edge.battery.fenecon.f2b.cluster.common.statemachine;

import io.openems.edge.battery.fenecon.f2b.cluster.common.statemachine.StateMachine.State;
import io.openems.edge.battery.fenecon.f2b.cluster.parallel.BatteryFeneconF2bClusterParallel;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;

public class StoppedHandler extends StateHandler<State, Context> {

	@Override
	protected void onEntry(Context context) {
		final var cluster = context.getParent();
		cluster._setOneBatteryNotRunning(false);
		cluster._setOneBatteryNotStopped(false);
		cluster._setOneBatteryStopped(false);
		if (cluster.isParallelCluster()) {
			((BatteryFeneconF2bClusterParallel) cluster)._setVoltageDifferenceHigh(false);
		}
	}

	@Override
	public State runAndGetNextState(Context context) {
		final var cluster = context.getParent();

		// Has Faults -> error handling
		if (cluster.hasFaults()) {
			return State.ERROR;
		}
		var hasFault = cluster.getAndUpdateHasAnyBatteryFault();
		if (hasFault && cluster.isSerialCluster()) {
			return State.ERROR;
		}

		// Check if all batteries are stopped
		if (!cluster.areAllBatteriesStopped() && cluster.isSerialCluster()) {
			cluster._setOneBatteryNotStopped(true);
			return State.ERROR;
		}

		if (cluster.getStartStopTarget() == StartStop.START) {
			return State.GO_RUNNING;
		}

		// Mark as stopped
		cluster._setStartStop(StartStop.STOP);
		return State.STOPPED;
	}
}
