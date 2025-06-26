package io.openems.edge.ess.generic.offgrid.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.batteryinverter.api.OffGridBatteryInverter.TargetGridMode;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.ess.generic.offgrid.statemachine.StateMachine.OffGridState;

/**
 * Reads the State of the Grid-Switch relays.
 *
 * <ul>
 * <li>If system is currently connected to grid: START_BATTERY_IN_ON_GRID
 * <li>If system is currently disconnected from grid: START_BATTERY_IN_OFF_GRID
 * </ul>
 */
public class GridSwitchHandler extends StateHandler<OffGridState, Context> {

	private Instant lastStateChange;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.lastStateChange = Instant.now(context.componentManager.getClock());
	}

	@Override
	protected OffGridState runAndGetNextState(Context context) throws OpenemsNamedException {
		final var offGridSwitch = context.offGridSwitch;
		final var inverter = context.batteryInverter;

		if (!context.isChannelsDefined()) {
			// Wait till MainContactor and GroundingContactor are defined.
			return OffGridState.GRID_SWITCH;
		}

		switch (offGridSwitch.getGridMode()) {
		case UNDEFINED:
			// Wait till GridStatus is defined.
			return OffGridState.GRID_SWITCH;
		case ON_GRID:
			if (context.isFromOffToOnGrid()) {
				this.changeFromOffToOnGrid(context);
			} else {
				// Inverter grid mode should set, before setting the
				// relays in required position
				inverter.setTargetGridMode(TargetGridMode.GO_ON_GRID);
				if (context.isOnGridContactorsSet()) {
					return OffGridState.START_BATTERY_IN_ON_GRID;
				}
				context.setContactorsForOnGrid();
			}
			return OffGridState.GRID_SWITCH;
		case OFF_GRID:
			inverter.setTargetGridMode(TargetGridMode.GO_OFF_GRID);
			if (context.isOffGridContactorsSet()) {
				return OffGridState.START_BATTERY_IN_OFF_GRID;
			}
			context.setContactorsForOffGrid();
			return OffGridState.GRID_SWITCH;
		}
		return OffGridState.GRID_SWITCH;
	}

	private void changeFromOffToOnGrid(Context context) throws OpenemsNamedException {
		var inverter = context.batteryInverter;
		var ess = context.getParent();
		if (inverter.isStopped()) {
			context.setFromOffToOnGrid(false);
		}

		if (Instant.now(context.componentManager.getClock()).minus(Duration.ofSeconds(60))
				.isAfter(this.lastStateChange)) {
			this.lastStateChange = Instant.now(context.componentManager.getClock());
			inverter.stop();
		}
		ess._setGridMode(GridMode.UNDEFINED);
	}
}
