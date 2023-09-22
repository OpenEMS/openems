package io.openems.edge.battery.fenecon.f2b.cluster.common.statemachine;

import static io.openems.edge.common.channel.ChannelUtils.getValues;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.fenecon.f2b.bmw.BatteryFeneconF2bBmwImpl.BatteryValues;
import io.openems.edge.battery.fenecon.f2b.cluster.common.Constants;
import io.openems.edge.battery.fenecon.f2b.cluster.common.statemachine.StateMachine.State;
import io.openems.edge.battery.fenecon.f2b.cluster.serial.BatteryFeneconF2bClusterSerial;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;

public class GoRunningHandler extends StateHandler<State, Context> {

	private Instant timeAtEntry = Instant.MIN;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.timeAtEntry = Instant.now(context.clock);
		context.getParent()._setMaxStartAttemptsFailed(false);
	}

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		final var cluster = context.getParent();
		if (context.batteries.isEmpty()) {
			return State.GO_RUNNING;
		}
		// Has Faults -> error handling
		if (cluster.hasFaults() || (cluster.getTimeoutStartBatteries().isDefined()//
				&& cluster.getTimeoutStartBatteries().get())) {
			return State.ERROR;
		}

		if (cluster.hasBatteriesFault()) {
			cluster._setAtLeastOneBatteryInError(true);
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

		if (cluster instanceof BatteryFeneconF2bClusterSerial) {
			var defined = context.batteries.stream().allMatch(t -> {
				return getValues(t, BatteryValues.class).isPresent();
			});

			if (!defined) {
				return State.GO_RUNNING;
			}
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
