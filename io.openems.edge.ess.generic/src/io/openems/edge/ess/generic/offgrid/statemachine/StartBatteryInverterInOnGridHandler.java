package io.openems.edge.ess.generic.offgrid.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.batteryinverter.api.OffGridBatteryInverter.TargetGridMode;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.ess.generic.common.GenericManagedEss;
import io.openems.edge.ess.generic.offgrid.statemachine.StateMachine.OffGridState;

public class StartBatteryInverterInOnGridHandler extends StateHandler<OffGridState, Context> {

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
	public OffGridState runAndGetNextState(Context context) throws OpenemsNamedException {
		final var ess = context.getParent();
		final var inverter = context.batteryInverter;

		if (context.batteryInverter.isStarted()) {
			return OffGridState.STARTED_IN_ON_GRID;
		}

		var isMaxStartTimePassed = Duration.between(this.lastAttempt, Instant.now())
				.getSeconds() > GenericManagedEss.RETRY_COMMAND_SECONDS;
		if (!isMaxStartTimePassed) {
			// Still waiting...
			return OffGridState.START_BATTERY_INVERTER_IN_ON_GRID;
		}
		if (this.attemptCounter > GenericManagedEss.RETRY_COMMAND_MAX_ATTEMPTS) {
			// Too many tries
			ess._setMaxBatteryInverterStartAttemptsFault(true);
			return OffGridState.UNDEFINED;

		} else {
			// Trying to start Battery
			inverter.setTargetGridMode(TargetGridMode.GO_ON_GRID);
			inverter.start();
			ess._setGridMode(GridMode.ON_GRID);

			this.lastAttempt = Instant.now();
			this.attemptCounter++;
			return OffGridState.START_BATTERY_INVERTER_IN_ON_GRID;
		}
	}
}
