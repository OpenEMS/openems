package io.openems.edge.battery.soltaro.cluster.versionc.statemachine;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.soltaro.cluster.enums.ClusterStartStop;
import io.openems.edge.battery.soltaro.cluster.enums.Rack;
import io.openems.edge.battery.soltaro.cluster.enums.RackUsage;
import io.openems.edge.battery.soltaro.cluster.versionc.statemachine.StateMachine.State;
import io.openems.edge.battery.soltaro.single.versionc.enums.PreChargeControl;
import io.openems.edge.battery.soltaro.versionc.utils.Constants;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.statemachine.StateHandler;

public class GoRunningHandler extends StateHandler<State, Context> {

	private Instant lastAttempt = Instant.MIN;
	private int attemptCounter = 0;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.lastAttempt = Instant.MIN;
		this.attemptCounter = 0;
		var battery = context.getParent();
		battery._setMaxStartAttempts(false);
	}

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		var battery = context.getParent();
		var commonPreChargeControl = battery.getCommonPreChargeControl().orElse(PreChargeControl.UNDEFINED);
		if (commonPreChargeControl == PreChargeControl.RUNNING) {
			return State.RUNNING;
		}

		var isMaxStartTimePassed = Duration.between(this.lastAttempt, Instant.now())
				.getSeconds() > Constants.RETRY_COMMAND_SECONDS;
		if (isMaxStartTimePassed) {
			// First try - or waited long enough for next try

			if (this.attemptCounter > Constants.RETRY_COMMAND_MAX_ATTEMPTS) {
				// Too many tries
				battery._setMaxStartAttempts(true);
				return State.UNDEFINED;

			} else {
				// Trying to switch on
				battery.setClusterStartStop(ClusterStartStop.START);

				// Set the active racks as 'USED', set the others as 'UNUSED'
				Set<Rack> activeRacks = battery.getRacks();
				for (Rack rack : Rack.values()) {
					EnumWriteChannel rackUsageChannel = battery.channel(rack.usageChannelId);
					if (activeRacks.contains(rack)) {
						rackUsageChannel.setNextWriteValue(RackUsage.USED);
					} else {
						rackUsageChannel.setNextWriteValue(RackUsage.UNUSED);
					}
				}

				this.lastAttempt = Instant.now();
				this.attemptCounter++;
				return State.GO_RUNNING;

			}

		} else {
			// Still waiting...
			return State.GO_RUNNING;
		}
	}

}
