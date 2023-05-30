package io.openems.edge.battery.statemachine;

import io.openems.edge.battery.statemachine.StateMachine.State;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;

public class StoppedHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		// Mark as stopped
		var battery = context.getParent();
		battery._setStartStop(StartStop.STOP);

		return State.STOPPED;
	}
}
