package io.openems.edge.battery.soltaro.cluster.versionc.statemachine;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.soltaro.cluster.enums.ContactorControl;
import io.openems.edge.battery.soltaro.cluster.enums.Rack;
import io.openems.edge.battery.soltaro.cluster.enums.RackUsage;
import io.openems.edge.battery.soltaro.cluster.enums.StartStop;
import io.openems.edge.battery.soltaro.cluster.versionc.statemachine.StateMachine.Context;
import io.openems.edge.battery.soltaro.versionc.utils.Constants;
import io.openems.edge.common.channel.EnumWriteChannel;

public class GoRunning extends State.Handler {

	private Instant lastAttempt = Instant.MIN;
	private int attemptCounter = 0;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.lastAttempt = Instant.MIN;
		this.attemptCounter = 0;
		context.component.setMaxStartAttempts(false);
	}

	@Override
	public State getNextState(Context context) throws OpenemsNamedException {
		ContactorControl commonContactorControlState = context.component.getCommonContactorControlState()
				.orElse(ContactorControl.UNDEFINED);
		if (commonContactorControlState == ContactorControl.ON_GRID) {
			return State.RUNNING;
		}

		boolean isMaxStartTimePassed = Duration.between(this.lastAttempt, Instant.now())
				.getSeconds() > Constants.RETRY_COMMAND_SECONDS;
		if (isMaxStartTimePassed) {
			// First try - or waited long enough for next try

			if (this.attemptCounter > Constants.RETRY_COMMAND_MAX_ATTEMPTS) {
				// Too many tries
				context.component.setMaxStartAttempts(true);
				return State.UNDEFINED;

			} else {
				// Trying to switch on
				context.component.setStartStop(StartStop.START);

				// Set the active racks as 'USED', set the others as 'UNUSED'
				Set<Rack> activeRacks = context.component.getRacks();
				for (Rack rack : Rack.values()) {
					EnumWriteChannel rackUsageChannel = context.component.channel(rack.usageChannelId);
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
