package io.openems.edge.battery.bydcommercial.statemachine;

import io.openems.edge.battery.bydcommercial.statemachine.StateMachine.State;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;

public class StoppedHandler extends StateHandler<State, Context> {
	@Override
	public State runAndGetNextState(Context context) {
		var battery = context.getParent();

		// Mark as stopped
		battery._setStartStop(StartStop.STOP);

		return State.STOPPED;
	}
}
