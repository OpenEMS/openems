package io.openems.edge.battery.soltaro.cluster.versionc.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.soltaro.cluster.enums.Rack;
import io.openems.edge.battery.soltaro.cluster.enums.RackUsage;
import io.openems.edge.battery.soltaro.cluster.enums.StartStop;
import io.openems.edge.battery.soltaro.cluster.versionc.statemachine.StateMachine.Context;
import io.openems.edge.common.channel.EnumWriteChannel;

public class ErrorHandling extends State.Handler {

	private Instant entryAt = Instant.MIN;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.entryAt = Instant.now();

		// Try to stop system
		context.component.setStartStop(StartStop.STOP);
		for (Rack rack : Rack.values()) {
			EnumWriteChannel rackUsageChannel = context.component.channel(rack.usageChannelId);
			rackUsageChannel.setNextWriteValue(RackUsage.UNUSED);
		}
	}

	@Override
	protected void onExit(Context context) throws OpenemsNamedException {
		context.component.setMaxStartAttempts(false);
		context.component.setMaxStopAttempts(false);
	}

	@Override
	public State getNextState(Context context) {
		System.out.println("Stuck in ERROR_HANDLING: " + context.component.getState().listStates());

		if (Duration.between(this.entryAt, Instant.now()).getSeconds() > 120) {
			// Try again
			return State.UNDEFINED;
		}

		return State.ERROR_HANDLING;
	}

}
