package io.openems.edge.goodwe.common.applypower;

import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.goodwe.common.applypower.ApplyPowerStateMachine.State;
import io.openems.edge.goodwe.common.enums.PowerModeEms;

public class EtDefaultHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		if (context.activePowerSetPoint > 0) {
			// Set-Point is positive
			if (context.activePowerSetPoint > context.pvProduction) {
				context.setMode(PowerModeEms.DISCHARGE_PV, context.activePowerSetPoint - context.pvProduction);
			} else {
				context.setMode(PowerModeEms.CHARGE_BAT, context.pvProduction - context.activePowerSetPoint);
			}

		} else {
			// Set-Point is negative or zero
			context.setMode(PowerModeEms.CHARGE_BAT, context.pvProduction - context.activePowerSetPoint);
		}

		// Evaluate next state
		if (context.soc > 99) {
			return State.ET_FULL;
		} else if (context.soc < 1) {
			return State.ET_EMPTY;
		} else {
			return State.ET_DEFAULT;
		}
	}

}
