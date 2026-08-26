package io.openems.edge.ess.generic.offgrid.statemachine;

import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.ess.generic.offgrid.statemachine.StateMachine.OffGridState;

public class UndefinedHandler extends StateHandler<OffGridState, Context> {

	@Override
	public OffGridState runAndGetNextState(Context context) {
		var ess = context.getParent();
		ess._setStartStop(StartStop.UNDEFINED);

		return switch (ess.getStartStopTarget()) {
		case UNDEFINED // Stuck in UNDEFINED State
			-> OffGridState.UNDEFINED;

		case START// force START
			-> ess.hasFaults()
					// Has Faults -> error handling
					? OffGridState.ERROR
					// No Faults -> Check the Relay States
					: OffGridState.GRID_SWITCH;

		case STOP // force STOP
			-> OffGridState.STOP_BATTERY_INVERTER;
		};
	}

}
