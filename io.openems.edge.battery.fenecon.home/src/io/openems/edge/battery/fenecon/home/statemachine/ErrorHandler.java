package io.openems.edge.battery.fenecon.home.statemachine;

import io.openems.edge.battery.fenecon.home.statemachine.StateMachine.State;

public class ErrorHandler extends StateMachine.BatteryStateHandler {

	@Override
	public State runAndGetNextState(Context context) {

		if (context.getParent().getLowMinVoltage().orElse(false)) {
			return State.GO_STOPPED;
		}

		if (context.getParent().hasFaults()) {
			return State.ERROR;
		} else {
			return State.UNDEFINED;
		}
	}

	@Override
	public boolean isChargeAllowed(Context context) {
		return false;
	}

	@Override
	public boolean isDischargeAllowed(Context context) {
		return false;
	}
}
