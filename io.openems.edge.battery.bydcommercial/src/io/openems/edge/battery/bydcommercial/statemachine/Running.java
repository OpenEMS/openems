package io.openems.edge.battery.bydcommercial.statemachine;

import io.openems.edge.battery.bydcommercial.PreChargeControl;
import io.openems.edge.battery.bydcommercial.statemachine.StateMachine.Context;

public class Running extends State.Handler {

	@Override
	public State getNextState(Context context) {
		if (context.component.hasFaults()) {
			return State.UNDEFINED;
		}

		if (context.component.getPreChargeControl() != PreChargeControl.RUNNING) {
			return State.UNDEFINED;
		}

		context.component._setReadyForWorking(true);

		return State.RUNNING;
	}

}
