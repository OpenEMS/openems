package io.openems.edge.battery.fenecon.home.statemachine;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.battery.fenecon.home.statemachine.StateMachine.State;

public class FirmwareUpdateHandler extends StateMachine.BatteryStateHandler {
	@Override
	protected State runAndGetNextState(Context context) throws OpenemsError.OpenemsNamedException {
		var battery = context.getParent();

		if (battery.isFirmwareUpdateRunning()) {
			return State.FIRMWARE_UPDATE;
		}

		return State.UNDEFINED;
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
