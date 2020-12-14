package io.openems.edge.battery.bydcommercial.statemachine;

import io.openems.edge.battery.bydcommercial.BatteryBoxC130;
import io.openems.edge.battery.bydcommercial.PowerCircuitControl;
import io.openems.edge.battery.bydcommercial.statemachine.StateMachine.State;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;

public class RunningHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		BatteryBoxC130 battery = context.getParent();

		if (battery.hasFaults()) {
			return State.UNDEFINED;
		}

		if (battery.getPowerCircuitControl() != PowerCircuitControl.SWITCH_ON) {
			return State.UNDEFINED;
		}

		// Mark as started
		battery._setStartStop(StartStop.START);

		return State.RUNNING;
	}

}
