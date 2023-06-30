package io.openems.edge.ess.generic.symmetric.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.ess.generic.common.GenericManagedEss;
import io.openems.edge.ess.generic.symmetric.statemachine.StateMachine.State;

public class StopBatteryHandler extends StateHandler<State, Context> {

	private Instant lastAttempt = Instant.MIN;
	private int attemptCounter = 0;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.lastAttempt = Instant.MIN;
		this.attemptCounter = 0;
		var ess = context.getParent();
		ess._setMaxBatteryStopAttemptsFault(false);
	}

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		var ess = context.getParent();

		if (context.battery.isStopped()) {
			return State.STOPPED;
		}

		var isMaxStartTimePassed = Duration.between(this.lastAttempt, Instant.now())
				.getSeconds() > GenericManagedEss.RETRY_COMMAND_SECONDS;
		if (!isMaxStartTimePassed) {
			// Still waiting...
			return State.STOP_BATTERY;
		}
		if (this.attemptCounter > GenericManagedEss.RETRY_COMMAND_MAX_ATTEMPTS) {
			// Too many tries
			ess._setMaxBatteryStopAttemptsFault(true);
			return State.UNDEFINED;

		} else {
			// Trying to stop Battery
			context.battery.stop();

			this.lastAttempt = Instant.now();
			this.attemptCounter++;
			return State.STOP_BATTERY;

		}
	}

}
