package io.openems.edge.battery.soltaro.cluster.versionc.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.soltaro.cluster.enums.ClusterStartStop;
import io.openems.edge.battery.soltaro.cluster.enums.Rack;
import io.openems.edge.battery.soltaro.cluster.enums.RackUsage;
import io.openems.edge.battery.soltaro.cluster.versionc.BatterySoltaroClusterVersionC;
import io.openems.edge.battery.soltaro.cluster.versionc.statemachine.StateMachine.State;
import io.openems.edge.battery.soltaro.single.versionc.enums.PreChargeControl;
import io.openems.edge.battery.soltaro.versionc.utils.Constants;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.statemachine.StateHandler;

public class GoStoppedHandler extends StateHandler<State, Context> {

	private Instant lastAttempt = Instant.MIN;
	private int attemptCounter = 0;

	@Override
	protected void onEntry(Context context) {
		this.lastAttempt = Instant.MIN;
		this.attemptCounter = 0;
	}

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		BatterySoltaroClusterVersionC battery = context.getParent();
		PreChargeControl commonPreChargeControl = battery.getCommonPreChargeControl()
				.orElse(PreChargeControl.UNDEFINED);
		if (commonPreChargeControl == PreChargeControl.SWITCH_OFF) {
			return State.STOPPED;
		}

		var isMaxStartTimePassed = Duration.between(this.lastAttempt, Instant.now())
				.getSeconds() > Constants.RETRY_COMMAND_SECONDS;
		if (isMaxStartTimePassed) {
			// First try - or waited long enough for next try

			if (this.attemptCounter > Constants.RETRY_COMMAND_MAX_ATTEMPTS) {
				// Too many tries
				battery._setMaxStopAttempts(true);
				return State.UNDEFINED;

			} else {
				// Trying to switch off
				battery.setClusterStartStop(ClusterStartStop.STOP);
				for (Rack rack : Rack.values()) {
					EnumWriteChannel rackUsageChannel = battery.channel(rack.usageChannelId);
					rackUsageChannel.setNextWriteValue(RackUsage.UNUSED);
				}
				this.lastAttempt = Instant.now();
				this.attemptCounter++;
				return State.GO_STOPPED;

			}

		} else {
			// Still waiting...
			return State.GO_STOPPED;
		}
	}

}
