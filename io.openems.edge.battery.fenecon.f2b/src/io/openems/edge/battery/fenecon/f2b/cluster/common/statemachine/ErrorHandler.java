package io.openems.edge.battery.fenecon.f2b.cluster.common.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.fenecon.f2b.cluster.common.statemachine.StateMachine.State;
import io.openems.edge.battery.fenecon.f2b.cluster.parallel.BatteryFeneconF2bClusterParallel;
import io.openems.edge.common.statemachine.StateHandler;

public class ErrorHandler extends StateHandler<State, Context> {

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		final var cluster = context.getParent();
		cluster.stopBatteries();
	}

	@Override
	protected void onExit(Context context) throws OpenemsNamedException {
		final var cluster = context.getParent();
		cluster._setTimeoutStartBatteries(false);
		cluster._setTimeoutStopBatteries(false);
	}

	@Override
	public State runAndGetNextState(Context context) {
		final var cluster = context.getParent();
		if (cluster instanceof BatteryFeneconF2bClusterParallel) {
			if (cluster.areAllBatteriesStopped()) {
				return State.GO_STOPPED;
			}
			return State.ERROR;
		}

		if (!cluster.hasFaults() && !cluster.getAndUpdateHasAnyBatteryFault()) {
			cluster._setOneBatteryNotRunning(false);
			return State.GO_STOPPED;
		}
		return State.ERROR;
	}

}
