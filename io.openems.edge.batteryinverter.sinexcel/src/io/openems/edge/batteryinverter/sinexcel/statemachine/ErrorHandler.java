package io.openems.edge.batteryinverter.sinexcel.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.batteryinverter.sinexcel.BatteryInverterSinexcel;
import io.openems.edge.batteryinverter.sinexcel.statemachine.StateMachine.State;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.statemachine.StateHandler;

public class ErrorHandler extends StateHandler<State, Context> {

	private static final int WAIT_SECONDS = 120;

	private Instant entryAt = Instant.MIN;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.entryAt = Instant.now();

		// Clear Failures
		this.setClearFailureCommand(context);

		// Try to stop systems
		final var inverter = context.getParent();
		inverter.setStopInverter();
	}

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		if (Duration.between(this.entryAt, Instant.now()).getSeconds() > WAIT_SECONDS) {
			// Try again
			return State.UNDEFINED;
		}

		// Wait
		return State.ERROR;
	}

	private void setClearFailureCommand(Context context) throws OpenemsNamedException {
		BooleanWriteChannel setClearFailureCmd = context.getParent()
				.channel(BatteryInverterSinexcel.ChannelId.CLEAR_FAILURE);
		setClearFailureCmd.setNextWriteValue(true); // 1: true, other: illegal
	}
}
