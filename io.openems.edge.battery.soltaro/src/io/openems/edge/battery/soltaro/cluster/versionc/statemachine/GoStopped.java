package io.openems.edge.battery.soltaro.cluster.versionc.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.soltaro.cluster.enums.ContactorControl;
import io.openems.edge.battery.soltaro.cluster.enums.Rack;
import io.openems.edge.battery.soltaro.cluster.enums.RackUsage;
import io.openems.edge.battery.soltaro.cluster.enums.StartStop;
import io.openems.edge.battery.soltaro.cluster.versionc.statemachine.StateMachine.Context;
import io.openems.edge.battery.soltaro.versionc.utils.Constants;
import io.openems.edge.common.channel.EnumWriteChannel;

public class GoStopped extends State.Handler {

	private Instant lastAttempt = Instant.MIN;
	private int attemptCounter = 0;

	@Override
	protected void onEntry(Context context) {
		this.lastAttempt = Instant.MIN;
		this.attemptCounter = 0;
	}

	@Override
	public State getNextState(Context context) throws OpenemsNamedException {
		ContactorControl commonContactorControlState = context.component.getCommonContactorControlState()
				.orElse(ContactorControl.UNDEFINED);
		if (commonContactorControlState == ContactorControl.CUT_OFF) {
			return State.STOPPED;
		}

		boolean isMaxStartTimePassed = Duration.between(this.lastAttempt, Instant.now())
				.getSeconds() > Constants.RETRY_COMMAND_SECONDS;
		if (isMaxStartTimePassed) {
			// First try - or waited long enough for next try

			if (this.attemptCounter > Constants.RETRY_COMMAND_MAX_ATTEMPTS) {
				// Too many tries
				context.component.setMaxStopAttempts(true);
				return State.UNDEFINED;

			} else {
				// Trying to switch off
				context.component.setStartStop(StartStop.STOP);
				for (Rack rack : Rack.values()) {
					EnumWriteChannel rackUsageChannel = context.component.channel(rack.usageChannelId);
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
