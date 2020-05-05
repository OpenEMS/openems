package io.openems.edge.ess.generic.symmetric.statemachine;

import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.ess.generic.symmetric.statemachine.Context;

public class Undefined extends StateHandler<State, Context> {

	@Override
	public State getNextState(Context context) {
		switch (context.component.getStartStopTarget()) {
		case UNDEFINED:
			// Stuck in UNDEFINED State
			return State.UNDEFINED;

		case START:
			// force START
			if (context.component.hasFaults()) {
				// Has Faults -> error handling
				return State.ERROR_HANDLING;
			} else {
				// No Faults -> start
				return State.START_BATTERY;
			}

		case STOP:
			// force STOP
//			TODO return State.STOP_BATTERY_INVERTER;
			return State.UNDEFINED;
		}

		assert false;
		return State.UNDEFINED; // can never happen
	}

}
