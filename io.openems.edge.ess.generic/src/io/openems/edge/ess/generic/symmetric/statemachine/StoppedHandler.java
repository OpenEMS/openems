package io.openems.edge.ess.generic.symmetric.statemachine;

import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.ess.generic.symmetric.statemachine.StateMachine.State;

public class StoppedHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		var ess = context.getParent();

		if (ess.hasFaults()) {
			return State.UNDEFINED;
		}

		if (!context.battery.isStopped()) {
			return State.UNDEFINED;
		}

		if (!context.batteryInverter.isStopped()) {
			return State.UNDEFINED;
		}

		// Mark as stopped
		ess._setStartStop(StartStop.STOP);

		return State.STOPPED;
	}

}
