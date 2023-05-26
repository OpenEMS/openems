package io.openems.edge.battery.soltaro.single.versionb.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.soltaro.single.versionb.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class UndefinedHandler extends StateHandler<State, Context> {

	@Override
	protected State runAndGetNextState(Context context) throws OpenemsNamedException {
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
