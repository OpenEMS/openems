package io.openems.edge.ess.generic.symmetric.statemachine;

import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;

public class Started extends StateHandler<State, Context> {

	@Override
	public State getNextState(Context context) {
		if (context.component.hasFaults()) {
			return State.UNDEFINED;
		}

		if (context.battery.getStartStop() != StartStop.START) {
			return State.UNDEFINED;
		}

		if (context.batteryInverter.getStartStop() != StartStop.START) {
			return State.UNDEFINED;
		}

		// Mark as started
		context.component._setStartStop(StartStop.START);

		return State.STARTED;
	}

}
