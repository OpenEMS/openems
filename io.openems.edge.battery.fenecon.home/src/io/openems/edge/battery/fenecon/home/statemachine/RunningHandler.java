package io.openems.edge.battery.fenecon.home.statemachine;

import io.openems.edge.battery.fenecon.home.statemachine.StateMachine.State;
import io.openems.edge.battery.poweramp.enums.BMSControl;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;

public class RunningHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		if (context.component.hasFaults()) {
			return State.UNDEFINED;
		}

		if (context.component.getBMSControl() != BMSControl.SWITCHED_ON) {
			return State.UNDEFINED;
		}

		// Mark as started
		context.component._setStartStop(StartStop.START);

		return State.RUNNING;
	}

}
