package io.openems.edge.battery.fenecon.home.statemachine;

import io.openems.edge.battery.fenecon.home.statemachine.StateMachine.State;
import io.openems.edge.common.startstop.StartStop;

public class RunningHandler extends StateMachine.BatteryStateHandler {

	@Override
	public State runAndGetNextState(Context context) {
		var battery = context.getParent();

		if (battery.isFirmwareUpdateRunning()) {
			return State.FIRMWARE_UPDATE;
		}

		if (battery.hasFaults()) {
			return State.ERROR;
		}

		// Is Battery still started?
		if (context.bmsControl != Boolean.TRUE) {
			return State.UNDEFINED;
		}

		// Mark as started
		battery._setStartStop(StartStop.START);

		return State.RUNNING;
	}

	@Override
	public boolean isChargeAllowed(Context context) {
		return true;
	}

	@Override
	public boolean isDischargeAllowed(Context context) {
		return true;
	}
}
