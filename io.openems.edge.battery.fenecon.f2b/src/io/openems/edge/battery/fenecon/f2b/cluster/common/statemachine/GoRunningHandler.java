package io.openems.edge.battery.fenecon.f2b.cluster.common.statemachine;

import static io.openems.edge.common.channel.ChannelUtils.getValues;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.fenecon.f2b.bmw.BatteryFeneconF2bBmwImpl.BatteryValues;
import io.openems.edge.battery.fenecon.f2b.cluster.common.Constants;
import io.openems.edge.battery.fenecon.f2b.cluster.common.statemachine.StateMachine.State;
import io.openems.edge.battery.fenecon.f2b.cluster.parallel.BatteryFeneconF2bClusterParallel;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;

public class GoRunningHandler extends StateHandler<State, Context> {

	private Instant timeAtEntry = Instant.MIN;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.timeAtEntry = Instant.now(context.clock);
		final var cluster = context.getParent();
		cluster._setMaxStartAttemptsFailed(false);
		if (cluster.isParallelCluster()) {
			((BatteryFeneconF2bClusterParallel) cluster)._setVoltageDifferenceHigh(false);
		}
	}

	@Override
	public State runAndGetNextState(Context context) {
		final var cluster = context.getParent();

		if (cluster.getAndUpdateHasAllBatteriesFault() || cluster.hasFaults()) {
			return State.ERROR;
		}

		var hasFault = cluster.getAndUpdateHasAnyBatteryFault();
		if (hasFault && cluster.isSerialCluster()) {
			return State.ERROR;
		}

		if (cluster.getStartStopTarget() == StartStop.STOP) {
			cluster.stopBatteries();
			return State.GO_STOPPED;
		}

		// Is max allowed start time passed ?
		var now = Instant.now(context.clock);
		if (Duration.between(this.timeAtEntry, now).getSeconds() > Constants.MAX_ALLOWED_START_TIME) {
			cluster._setTimeoutStartBatteries(true);
		}

		try {
			cluster.startBatteries();
		} catch (OpenemsNamedException e) {
			cluster.stopBatteries();
			return State.ERROR;
		}

		var areBatteryValuesDefined = context.batteries.stream().allMatch(t -> {
			return getValues(t, BatteryValues.class).isPresent();
		});

		if (!areBatteryValuesDefined && cluster.isSerialCluster()) {
			return State.GO_RUNNING;
		}

		for (var battery : cluster.getNotStartedBatteries()) {
			battery.setHvContactorUnlocked(cluster.isHvContactorUnlocked());
			return State.GO_RUNNING;
		}

		// Check if all batteries are started
		if (cluster.areAllBatteriesStarted()) {
			return State.RUNNING;
		}
		return State.GO_RUNNING;
	}
}
