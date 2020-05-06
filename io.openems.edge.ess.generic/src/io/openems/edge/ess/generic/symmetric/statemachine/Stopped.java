package io.openems.edge.ess.generic.symmetric.statemachine;

import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;

public class Stopped extends StateHandler<State, Context> {

	@Override
	public State getNextState(Context context) {
		if (context.component.hasFaults()) {
			return State.UNDEFINED;
		}

		if (context.battery.getStartStop() != StartStop.STOP) {
			return State.UNDEFINED;
		}

		if (context.batteryInverter.getStartStop() != StartStop.STOP) {
			return State.UNDEFINED;
		}

		// Mark as stopped
		context.component._setStartStop(StartStop.STOP);

		return State.STOPPED;
	}

}
