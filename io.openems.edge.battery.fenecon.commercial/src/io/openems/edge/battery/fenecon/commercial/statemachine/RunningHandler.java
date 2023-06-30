package io.openems.edge.battery.fenecon.commercial.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.fenecon.commercial.statemachine.StateMachine.State;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;

public class RunningHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		var battery = context.getParent();

		if (battery.hasFaults()) {
			return State.UNDEFINED;
		}

		// Mark as started
		battery._setStartStop(StartStop.START);

		return State.RUNNING;
	}

}
