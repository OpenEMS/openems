package io.openems.edge.battery.fenecon.home.statemachine;

import io.openems.edge.battery.fenecon.home.statemachine.StateMachine.State;

public class UndefinedHandler extends StateMachine.BatteryStateHandler {

	@Override
	public State runAndGetNextState(Context context) {
		var battery = context.getParent();

		return switch (battery.getStartStopTarget()) {
		case UNDEFINED ->
			// Stuck in UNDEFINED State
			State.UNDEFINED;

		case START -> {
			// force START
			if (battery.isFirmwareUpdateRunning()) {
				yield State.FIRMWARE_UPDATE;
			}

			if (battery.getModbusCommunicationFailed()) {
				// Modbus Communication Failed -> try to start
				yield State.GO_RUNNING;

			} else if (battery.hasFaults()) {

				// Has Faults -> error handling
				yield State.ERROR;
			} else {
				// No Faults -> start
				yield State.GO_RUNNING;
			}
		}

		case STOP ->
			// force STOP
			State.GO_STOPPED;
		};
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
