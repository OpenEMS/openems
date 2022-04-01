package io.openems.edge.ess.generic.offgrid.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.ess.generic.offgrid.statemachine.StateMachine.OffGridState;

public class ErrorHandler extends StateHandler<OffGridState, Context> {

	private Instant entryAt = Instant.MIN;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.entryAt = Instant.now();

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
	public OffGridState runAndGetNextState(Context context) {
		if (Duration.between(this.entryAt, Instant.now()).getSeconds() > 120) {
			// Try again
			return OffGridState.UNDEFINED;
		}

		return OffGridState.ERROR;
	}

}
