package io.openems.edge.ess.generic.symmetric.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.ess.generic.symmetric.GenericManagedSymmetricEss;
import io.openems.edge.ess.generic.symmetric.statemachine.StateMachine.State;

public class StopBatteryInverterHandler extends StateHandler<State, Context> {

	private Instant lastAttempt = Instant.MIN;
	private int attemptCounter = 0;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.lastAttempt = Instant.MIN;
		this.attemptCounter = 0;
		context.component._setMaxBatteryInverterStopAttemptsFault(false);
	}

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		if (context.batteryInverter.isStopped()) {
			return State.STOP_BATTERY;
		}

		boolean isMaxStartTimePassed = Duration.between(this.lastAttempt, Instant.now())
				.getSeconds() > GenericManagedSymmetricEss.RETRY_COMMAND_SECONDS;
		if (isMaxStartTimePassed) {
			// First try - or waited long enough for next try

			if (this.attemptCounter > GenericManagedSymmetricEss.RETRY_COMMAND_MAX_ATTEMPTS) {
				// Too many tries
				context.component._setMaxBatteryInverterStopAttemptsFault(true);
				return State.UNDEFINED;

			} else {
				// Trying to stop Battery Inverter
				context.batteryInverter.stop();

				this.lastAttempt = Instant.now();
				this.attemptCounter++;
				return State.STOP_BATTERY_INVERTER;

			}

		} else {
			// Still waiting...
			return State.STOP_BATTERY_INVERTER;
		}
	}

}
