package io.openems.edge.ess.generic.offgrid.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.ess.generic.offgrid.statemachine.StateMachine.OffGridState;

public class StartedInOffGridHandler extends StateHandler<OffGridState, Context> {

	private static final int TARGET_GRID_FREQUENCY = 52; // Hz

	@Override
	public OffGridState runAndGetNextState(Context context) throws OpenemsNamedException {
		final var ess = context.getParent();
		final var battery = context.battery;
		final var inverter = context.batteryInverter;

		if (ess.hasFaults()) {
			return OffGridState.UNDEFINED;
		}

		if (!battery.isStarted()) {
			return OffGridState.UNDEFINED;
		}

		if (!inverter.isStarted()) {
			return OffGridState.UNDEFINED;
		}

		inverter.setTargetOffGridFrequency(TARGET_GRID_FREQUENCY);
		ess._setStartStop(StartStop.START);
		ess._setGridMode(GridMode.OFF_GRID);
		return OffGridState.STARTED_IN_OFF_GRID;
	}
}
