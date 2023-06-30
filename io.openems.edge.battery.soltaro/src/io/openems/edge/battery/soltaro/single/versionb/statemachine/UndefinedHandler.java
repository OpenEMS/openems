package io.openems.edge.battery.soltaro.single.versionb.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.soltaro.single.versionb.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class UndefinedHandler extends StateHandler<State, Context> {

	@Override
	protected State runAndGetNextState(Context context) throws OpenemsNamedException {
		var battery = context.getParent();

		switch (battery.getStartStopTarget()) {
		case UNDEFINED:
			// Stuck in UNDEFINED State
			return State.UNDEFINED;

		case START:
			// force START
			if (battery.hasFaults()) {
				// Has Faults -> error handling
				return State.ERROR;
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
