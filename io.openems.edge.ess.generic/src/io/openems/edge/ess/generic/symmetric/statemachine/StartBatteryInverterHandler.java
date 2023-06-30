package io.openems.edge.ess.generic.symmetric.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.ess.generic.common.GenericManagedEss;
import io.openems.edge.ess.generic.symmetric.statemachine.StateMachine.State;

public class StartBatteryInverterHandler extends StateHandler<State, Context> {

	private Instant lastAttempt = Instant.MIN;
	private int attemptCounter = 0;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.lastAttempt = Instant.MIN;
		this.attemptCounter = 0;
		var ess = context.getParent();
		ess._setMaxBatteryInverterStartAttemptsFault(false);
	}

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		var ess = context.getParent();

		if (context.batteryInverter.isStarted()) {
			return State.STARTED;
		}

		var isMaxStartTimePassed = Duration.between(this.lastAttempt, Instant.now())
				.getSeconds() > GenericManagedEss.RETRY_COMMAND_SECONDS;
		if (!isMaxStartTimePassed) {
			// Still waiting...
			return State.START_BATTERY_INVERTER;
		}
		if (this.attemptCounter > GenericManagedEss.RETRY_COMMAND_MAX_ATTEMPTS) {
			// Too many tries
			ess._setMaxBatteryInverterStartAttemptsFault(true);
			return State.UNDEFINED;

		} else {
			// Trying to start Battery
			context.batteryInverter.start();

			this.lastAttempt = Instant.now();
			this.attemptCounter++;
			return State.START_BATTERY_INVERTER;

		}
	}

}
