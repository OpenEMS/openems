package io.openems.edge.battery.soltaro.single.versionb.statemachine;

import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;

public class StoppedHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		if (context.component.hasFaults()) {
			return State.UNDEFINED;
		}

		if (!context.component.isSystemStopped()) {
			return State.UNDEFINED;
		}

		// Mark as stopped
		context.component._setStartStop(StartStop.STOP);

		return State.STOPPED;
	}

}
