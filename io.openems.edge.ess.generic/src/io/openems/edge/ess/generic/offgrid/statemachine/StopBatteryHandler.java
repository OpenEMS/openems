package io.openems.edge.ess.generic.offgrid.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.ess.generic.common.GenericManagedEss;
import io.openems.edge.ess.generic.offgrid.statemachine.StateMachine.OffGridState;

public class StopBatteryHandler extends StateHandler<OffGridState, Context> {

	private Instant lastAttempt = Instant.MIN;
	private int attemptCounter = 0;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.lastAttempt = Instant.MIN;
		this.attemptCounter = 0;
		GenericManagedEss ess = context.getParent();
		ess._setMaxBatteryStopAttemptsFault(false);
	}

	@Override
	public OffGridState runAndGetNextState(Context context) throws OpenemsNamedException {
		final GenericManagedEss ess = context.getParent();
		final Battery battery = context.battery;

		if (battery.isStopped()) {
			return OffGridState.STOPPED;
		}

		boolean isMaxStartTimePassed = Duration.between(this.lastAttempt, Instant.now())
				.getSeconds() > GenericManagedEss.RETRY_COMMAND_SECONDS;
		if (isMaxStartTimePassed) {
			// First try - or waited long enough for next try

			if (this.attemptCounter > GenericManagedEss.RETRY_COMMAND_MAX_ATTEMPTS) {
				// Too many tries
				ess._setMaxBatteryStopAttemptsFault(true);
				return OffGridState.UNDEFINED;

			} else {
				// Trying to stop Battery
				battery.stop();

				this.lastAttempt = Instant.now();
				this.attemptCounter++;
				return OffGridState.STOP_BATTERY;
			}
		} else {
			// Still waiting...
			return OffGridState.STOP_BATTERY;
		}
	}
}
