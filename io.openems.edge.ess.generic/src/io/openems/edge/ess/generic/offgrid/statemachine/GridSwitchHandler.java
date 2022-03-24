package io.openems.edge.ess.generic.offgrid.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.ess.generic.offgrid.statemachine.StateMachine.OffGridState;
import io.openems.edge.ess.offgrid.api.OffGridSwitch.Contactor;

/**
 * Reads the State of the Grid-Switch relays.
 *
 * <ul>
 * <li>If system is currently connected to grid: START_BATTERY_IN_ON_GRID
 * <li>If system is currently disconnected from grid: START_BATTERY_IN_OFF_GRID
 * </ul>
 */
public class GridSwitchHandler extends StateHandler<OffGridState, Context> {

	@Override
	protected OffGridState runAndGetNextState(Context context) throws OpenemsNamedException {
		final var offGridSwitch = context.offGridSwitch;

		if (!offGridSwitch.getMainContactor().isPresent() || !offGridSwitch.getGroundingContactor().isPresent()) {
			// Wait till MainContactor and GroundingContactor are defined.
			return OffGridState.GRID_SWITCH;
		}

		var mainContactor = offGridSwitch.getMainContactor().get();
		var groundingContactor = offGridSwitch.getGroundingContactor().get();
		switch (offGridSwitch.getGridMode()) {
		case UNDEFINED:
			// Wait till GridStatus is defined.
			return OffGridState.GRID_SWITCH;

		case ON_GRID:
			if (mainContactor == Contactor.CLOSE && groundingContactor == Contactor.OPEN) {
				// Correct contactor state for ON-GRID
				return OffGridState.START_BATTERY_IN_ON_GRID;
			}
			offGridSwitch.setMainContactor(Contactor.CLOSE);
			offGridSwitch.setGroundingContactor(Contactor.OPEN);
			return OffGridState.GRID_SWITCH;

		case OFF_GRID:
			if (mainContactor == Contactor.OPEN && groundingContactor == Contactor.CLOSE) {
				// Correct relays state for OFF-GRID
				return OffGridState.START_BATTERY_IN_OFF_GRID;
			}
			offGridSwitch.setMainContactor(Contactor.OPEN);
			offGridSwitch.setGroundingContactor(Contactor.CLOSE);
			return OffGridState.GRID_SWITCH;

		}
		return OffGridState.GRID_SWITCH;
	}
}
