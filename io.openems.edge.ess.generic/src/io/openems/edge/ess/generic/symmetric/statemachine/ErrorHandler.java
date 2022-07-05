package io.openems.edge.ess.generic.symmetric.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.ess.generic.symmetric.statemachine.StateMachine.State;

public class ErrorHandler extends StateHandler<State, Context> {

	private static final int WAIT_TIME_IN_SECONDS = 120;

	private Instant entryAt = Instant.MIN;
	private int startAttemptCounter = 0;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.entryAt = Instant.now();
		this.startAttemptCounter++;
		// Try to stop systems
		context.battery.setStartStop(StartStop.STOP);
		context.batteryInverter.setStartStop(StartStop.STOP);
	}

	@Override
	protected void onExit(Context context) throws OpenemsNamedException {
		var ess = context.getParent();

		ess._setMaxBatteryStartAttemptsFault(false);
		ess._setMaxBatteryStopAttemptsFault(false);
		ess._setMaxBatteryInverterStartAttemptsFault(false);
		ess._setMaxBatteryInverterStopAttemptsFault(false);
	}

	@Override
	public State runAndGetNextState(Context context) {
		if (Duration.between(this.entryAt, Instant.now()).getSeconds() > WAIT_TIME_IN_SECONDS
				* Math.pow(16, this.startAttemptCounter)) {
			// Try again
			return State.UNDEFINED;
		}

		return State.ERROR;
	}

}
