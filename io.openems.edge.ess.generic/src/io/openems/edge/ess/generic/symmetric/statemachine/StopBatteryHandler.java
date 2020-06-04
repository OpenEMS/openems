package io.openems.edge.ess.generic.symmetric.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.ess.generic.symmetric.GenericManagedSymmetricEss;

public class StopBatteryHandler extends StateHandler<State, Context> {

	private Instant lastAttempt = Instant.MIN;
	private int attemptCounter = 0;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.lastAttempt = Instant.MIN;
		this.attemptCounter = 0;
		context.component._setMaxBatteryStopAttemptsFault(false);
	}

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		if (context.battery.isStopped()) {
			return State.STOPPED;
		}

		boolean isMaxStartTimePassed = Duration.between(this.lastAttempt, Instant.now())
				.getSeconds() > GenericManagedSymmetricEss.RETRY_COMMAND_SECONDS;
		if (isMaxStartTimePassed) {
			// First try - or waited long enough for next try

			if (this.attemptCounter > GenericManagedSymmetricEss.RETRY_COMMAND_MAX_ATTEMPTS) {
				// Too many tries
				context.component._setMaxBatteryStopAttemptsFault(true);
				return State.UNDEFINED;

			} else {
				// Trying to stop Battery
				context.battery.stop();

				this.lastAttempt = Instant.now();
				this.attemptCounter++;
				return State.STOP_BATTERY;

			}

		} else {
			// Still waiting...
			return State.STOP_BATTERY;
		}
	}

}
