package io.openems.edge.ess.generic.offgrid.statemachine;

import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.ess.generic.offgrid.statemachine.StateMachine.OffGridState;

public class UndefinedHandler extends StateHandler<OffGridState, Context> {

	@Override
	public OffGridState runAndGetNextState(Context context) {
		var ess = context.getParent();
		ess._setStartStop(StartStop.UNDEFINED);

		switch (ess.getStartStopTarget()) {
		case UNDEFINED:
			// Stuck in UNDEFINED State
			return OffGridState.UNDEFINED;

		case START:
			// force START
			if (ess.hasFaults()) {
				// Has Faults -> error handling
				return OffGridState.ERROR;
			} else {
				// No Faults -> Check the Relay States
				return OffGridState.GRID_SWITCH;
			}

		case STOP:
			// force STOP
			return OffGridState.STOP_BATTERY_INVERTER;
		}

		assert false;
		return OffGridState.UNDEFINED; // can never happen
	}

}
