package io.openems.edge.battery.bydcommercial.statemachine;

import io.openems.edge.battery.bydcommercial.enums.PowerCircuitControl;
import io.openems.edge.battery.bydcommercial.statemachine.StateMachine.State;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;

public class RunningHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		if (context.getParent().hasFaults()) {
			return State.UNDEFINED;
		}

		if (context.getParent().getPowerCircuitControl() != PowerCircuitControl.SWITCH_ON) {
			return State.UNDEFINED;
		}

		// Mark as started
		context.getParent()._setStartStop(StartStop.START);

		return State.RUNNING;
	}

}
