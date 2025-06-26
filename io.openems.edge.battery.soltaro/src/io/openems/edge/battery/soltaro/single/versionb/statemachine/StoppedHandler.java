package io.openems.edge.battery.soltaro.single.versionb.statemachine;

import io.openems.edge.battery.soltaro.single.versionb.statemachine.StateMachine.State;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;

public class StoppedHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		var battery = context.getParent();

		if (battery.hasFaults()) {
			return State.UNDEFINED;
		}

		if (!ControlAndLogic.isSystemStopped(context.getParent())) {
			return State.UNDEFINED;
		}

		// Mark as stopped
		battery._setStartStop(StartStop.STOP);

		return State.STOPPED;
	}

}
