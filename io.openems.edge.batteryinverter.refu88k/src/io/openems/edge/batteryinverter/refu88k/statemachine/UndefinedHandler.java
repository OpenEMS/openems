package io.openems.edge.batteryinverter.refu88k.statemachine;

import io.openems.edge.batteryinverter.refu88k.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class UndefinedHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		var inverter = context.getParent();
		return switch (inverter.getStartStopTarget()) {
		case UNDEFINED // Stuck in UNDEFINED State
			-> State.UNDEFINED;

		case START // force START
			-> inverter.hasFaults() //
					// Has Faults -> error handling
					? State.ERROR
					// No Faults -> start
					: State.GO_RUNNING;

		case STOP // force STOP
			-> State.GO_STOPPED;
		};
	}
}
