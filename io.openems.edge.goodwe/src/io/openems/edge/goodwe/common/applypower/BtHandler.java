package io.openems.edge.goodwe.common.applypower;

import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.goodwe.common.applypower.ApplyPowerStateMachine.State;
import io.openems.edge.goodwe.common.enums.PowerModeEms;

public class BtHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		if (context.activePowerSetPoint > 0) {
			// Set-Point is positive
			context.setMode(PowerModeEms.DISCHARGE_BAT, context.activePowerSetPoint);

		} else {
			// Set-Point is negative or zero
			context.setMode(PowerModeEms.CHARGE_BAT, context.activePowerSetPoint * -1);
		}

		return State.BT;
	}

}
