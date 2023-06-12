package io.openems.edge.battery.statemachine;

import io.openems.edge.battery.statemachine.StateMachine.State;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;

public class StoppedHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		final var battery = context.getParent();
		// Has Faults -> error handling
		if (battery.hasFaults()) {
			return State.ERROR;
		}

		if (battery.getStartStopTarget() == StartStop.START) {
			return State.GO_RUNNING;
		}
		battery._setStartStop(StartStop.STOP);
		return State.STOPPED;
	}
}
