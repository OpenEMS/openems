package io.openems.edge.ess.generic.symmetric.statemachine;

import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.ess.generic.symmetric.statemachine.StateMachine.State;

public class UndefinedHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		var ess = context.getParent();
		switch (ess.getStartStopTarget()) {
		case UNDEFINED:
			// Stuck in UNDEFINED State
			return State.UNDEFINED;

		case START:
			// force START
			if (ess.hasFaults()) {
				// TODO should we consider also Battery-Inverter and Battery Faults?
				// TODO should the Modbus-Device also be on error, when then Modbus-Bridge is on
				// error?

				// Has Faults -> error handling
				return State.ERROR;
			} else {
				// No Faults -> start
				return State.START_BATTERY;
			}

		case STOP:
			// force STOP
			return State.STOP_BATTERY_INVERTER;
		}

		assert false;
		return State.UNDEFINED; // can never happen
	}

}
