package io.openems.edge.battery.soltaro.single.versionc.statemachine;

import io.openems.edge.battery.soltaro.single.versionc.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class UndefinedHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		var battery = context.getParent();
		return switch (battery.getStartStopTarget()) {
		case UNDEFINED ->
			// Stuck in UNDEFINED State
			 State.UNDEFINED;
		case START -> {
			// force START
			if (battery.hasFaults()) {
				// Has Faults -> error handling
				yield State.ERROR;
			} else {
				// No Faults -> start
				yield State.GO_RUNNING;
			}
		}
		case STOP ->
			// force STOP
			 State.GO_STOPPED;
		default -> {
			assert false;
			yield State.UNDEFINED; // can never happen
		}
		};
	}

}
