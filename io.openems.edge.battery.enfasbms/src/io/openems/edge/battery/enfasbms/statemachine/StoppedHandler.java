package io.openems.edge.battery.enfasbms.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.enfasbms.statemachine.StateMachine.State;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;

public class StoppedHandler extends StateHandler<State, Context> {

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		var battery = context.getParent();
		battery._setStartStop(StartStop.STOP);
	}

	@Override
	public State runAndGetNextState(Context context) {
		var battery = context.getParent();

		if (battery.getStartStopTarget() == StartStop.START) {
			return State.GO_RUNNING;
		}
		battery._setStartStop(StartStop.STOP);
		return State.STOPPED;

	}
}