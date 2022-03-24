package io.openems.edge.ess.generic.offgrid.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.batteryinverter.api.OffGridBatteryInverter.TargetGridMode;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.ess.generic.offgrid.statemachine.StateMachine.OffGridState;

public class StartedInOnGridHandler extends StateHandler<OffGridState, Context> {

	@Override
	public OffGridState runAndGetNextState(Context context) throws OpenemsNamedException {
		final var ess = context.getParent();
		final var battery = context.battery;
		final var inverter = context.batteryInverter;
		final var offGridSwitch = context.offGridSwitch;

		if (ess.hasFaults()) {
			return OffGridState.UNDEFINED;
		}

		if (!battery.isStarted()) {
			return OffGridState.UNDEFINED;
		}

		if (!inverter.isStarted()) {
			return OffGridState.UNDEFINED;
		}

		// Grid is Off
		// TODO move this logic to GridSwitch
		if (offGridSwitch.getGridMode() == GridMode.OFF_GRID) {
			inverter.setTargetGridMode(TargetGridMode.GO_OFF_GRID);
			return OffGridState.GRID_SWITCH;
		}

		ess._setGridMode(GridMode.ON_GRID);
		ess._setStartStop(StartStop.START);
		return OffGridState.STARTED_IN_ON_GRID;

	}
}
