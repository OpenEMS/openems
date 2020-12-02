package io.openems.edge.ess.generic.common.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.ess.generic.common.GenericManagedEss;
import io.openems.edge.ess.generic.common.statemachine.StateMachine.State;

public class StartBatteryHandler extends StateHandler<State, Context> {

	private Instant lastAttempt = Instant.MIN;
	private int attemptCounter = 0;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.lastAttempt = Instant.MIN;
		this.attemptCounter = 0;
		context.component._setMaxBatteryStartAttemptsFault(false);
	}

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		if (context.battery.isStarted()) {
			// TODO should we check here the other parameters defined in Battery Nature.
			return State.START_BATTERY_INVERTER;
		}

		boolean isMaxStartTimePassed = Duration.between(this.lastAttempt, Instant.now())
				.getSeconds() > GenericManagedEss.RETRY_COMMAND_SECONDS;
		if (isMaxStartTimePassed) {
			// First try - or waited long enough for next try

			if (this.attemptCounter > GenericManagedEss.RETRY_COMMAND_MAX_ATTEMPTS) {
				// Too many tries
				context.component._setMaxBatteryStartAttemptsFault(true);
				return State.UNDEFINED;

			} else {
				// Trying to start Battery
				context.battery.start();

				this.lastAttempt = Instant.now();
				this.attemptCounter++;
				return State.START_BATTERY;

			}

		} else {
			// Still waiting...
			return State.START_BATTERY;
		}
	}

}
