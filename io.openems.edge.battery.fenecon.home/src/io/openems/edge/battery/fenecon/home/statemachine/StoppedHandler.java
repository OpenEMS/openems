package io.openems.edge.battery.fenecon.home.statemachine;

import io.openems.edge.battery.fenecon.home.statemachine.StateMachine.State;
import io.openems.edge.common.startstop.StartStop;

public class StoppedHandler extends StateMachine.BatteryStateHandler {

	@Override
	public State runAndGetNextState(Context context) {
		final var battery = context.getParent();

		battery._setStartStop(StartStop.STOP);
		return State.STOPPED;
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
