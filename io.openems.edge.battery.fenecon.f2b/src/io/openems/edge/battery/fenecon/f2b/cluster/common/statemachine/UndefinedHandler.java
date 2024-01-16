package io.openems.edge.battery.fenecon.f2b.cluster.common.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.battery.fenecon.f2b.cluster.common.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class UndefinedHandler extends StateHandler<State, Context> {

	private final Logger log = LoggerFactory.getLogger(UndefinedHandler.class);

	@Override
	public State runAndGetNextState(Context context) {
		final var cluster = context.getParent();
		if (context.batteries.isEmpty()) {
			context.logInfo(this.log, "Battery list is empty, not found any battery to start");
			return State.ERROR;
		}

		if (cluster.getAndUpdateHasAnyBatteryFault() && cluster.isSerialCluster()) {
			return State.ERROR;
		}

		if (cluster.isOneBatteryStartedAndOneStopped()) {
			return State.GO_STOPPED;
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
