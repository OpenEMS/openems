package io.openems.edge.ess.generic.offgrid.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.batteryinverter.api.OffGridBatteryInverter.TargetGridMode;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.ess.generic.common.GenericManagedEss;
import io.openems.edge.ess.generic.offgrid.statemachine.StateMachine.OffGridState;

public class StartBatteryInverterInOffGridHandler extends StateHandler<OffGridState, Context> {

	private Instant lastAttempt = Instant.MIN;
	private int attemptCounter = 0;
	private static final int TARGET_OFF_GRID_FREQUENCY = 52;

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

		// Inverter is on
		if (context.batteryInverter.isStarted()) {
			return OffGridState.STARTED_IN_OFF_GRID;
		}

		var isMaxStartTimePassed = Duration.between(this.lastAttempt, Instant.now())
				.getSeconds() > GenericManagedEss.RETRY_COMMAND_SECONDS;
		if (!isMaxStartTimePassed) {
			// Still waiting...
			return OffGridState.START_BATTERY_INVERTER_IN_OFF_GRID;
		}
		if (this.attemptCounter > GenericManagedEss.RETRY_COMMAND_MAX_ATTEMPTS) {
			// Too many tries
			ess._setMaxBatteryInverterStartAttemptsFault(true);
			return OffGridState.UNDEFINED;

		} else {
			// Trying to start Battery
			inverter.setTargetGridMode(TargetGridMode.GO_OFF_GRID);
			inverter.setTargetOffGridFrequency(TARGET_OFF_GRID_FREQUENCY);
			inverter.start();
			ess._setGridMode(GridMode.OFF_GRID);

			this.lastAttempt = Instant.now();
			this.attemptCounter++;
			return OffGridState.START_BATTERY_INVERTER_IN_OFF_GRID;
		}
	}
}
