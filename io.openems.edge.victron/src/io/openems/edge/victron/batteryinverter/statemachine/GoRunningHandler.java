package io.openems.edge.victron.batteryinverter.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.batteryinverter.api.OffGridBatteryInverter.TargetGridMode;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.victron.batteryinverter.statemachine.StateMachine.State;

/**
 * Handles the GO_RUNNING state - transition from stopped/undefined to running.
 *
 * <p>
 * Victron inverters are typically "always running" when connected via Modbus,
 * so this handler primarily sets the grid mode and transitions to RUNNING.
 */
public class GoRunningHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		final var inverter = context.getParent();

		// Check for faults before proceeding
		if (inverter.hasFaults()) {
			return State.ERROR;
		}

		// Set grid mode based on target from context
		// Victron supports both ON_GRID and OFF_GRID operation
		if (context.targetGridMode == TargetGridMode.GO_ON_GRID) {
			inverter._setGridMode(GridMode.ON_GRID);
		} else if (context.targetGridMode == TargetGridMode.GO_OFF_GRID) {
			inverter._setGridMode(GridMode.OFF_GRID);
		}

		// Enable soft start for smooth power ramp-up
		inverter.softStart(true);

		// Victron is ready when Modbus connection is established
		// No explicit start command needed - transition directly to RUNNING
		return State.RUNNING;
	}
}
