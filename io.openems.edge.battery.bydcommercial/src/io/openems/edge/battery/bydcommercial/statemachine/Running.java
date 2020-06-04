package io.openems.edge.battery.bydcommercial.statemachine;

import io.openems.edge.battery.bydcommercial.PreChargeControl;
import io.openems.edge.battery.bydcommercial.statemachine.StateMachine.Context;
import io.openems.edge.common.startstop.StartStop;

public class Running extends State.Handler {

	@Override
	public State getNextState(Context context) {
		if (context.component.hasFaults()) {
			return State.UNDEFINED;
		}

		if (context.component.getPreChargeControl() != PreChargeControl.RUNNING) {
			return State.UNDEFINED;
		}

		// Mark as started
		context.component._setStartStop(StartStop.START);

		return State.RUNNING;
	}

}
