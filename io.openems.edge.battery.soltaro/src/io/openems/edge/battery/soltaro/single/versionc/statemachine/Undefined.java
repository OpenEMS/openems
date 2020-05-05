package io.openems.edge.battery.soltaro.single.versionc.statemachine;

import io.openems.edge.common.statemachine.StateHandler;

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
				return State.GO_RUNNING;
			}

		case STOP:
			// force STOP
			return State.GO_STOPPED;
		}

		assert false;
		return State.UNDEFINED; // can never happen
	}

}
