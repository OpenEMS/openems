package io.openems.edge.battery.fenecon.f2b.cluster.common.statemachine;

import io.openems.common.exceptions.InvalidValueException;
import io.openems.edge.battery.fenecon.f2b.cluster.common.statemachine.StateMachine.State;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;

public class RunningHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) throws InvalidValueException, IllegalArgumentException {
		final var cluster = context.getParent();

		// Has Faults -> error handling
		if (cluster.hasFaults()) {
			return State.ERROR;
		}

		if (cluster.hasBatteriesFault()) {
			cluster._setAtLeastOneBatteryInError(true);
			return State.ERROR;
		}

		if (!cluster.areAllBatteriesStarted()) {
			cluster._setAtLeastOneBatteryNotRunning(true);
			return State.ERROR;
		}

		if (cluster.getStartStopTarget() == StartStop.STOP) {
			return State.GO_STOPPED;
		}

		// Mark as started
		cluster._setStartStop(StartStop.START);
		cluster._setAtLeastOneBatteryNotRunning(false);
		return State.RUNNING;
	}
}
