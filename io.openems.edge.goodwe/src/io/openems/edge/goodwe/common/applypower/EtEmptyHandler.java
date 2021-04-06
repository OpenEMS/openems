package io.openems.edge.goodwe.common.applypower;

import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.goodwe.common.applypower.ApplyPowerStateMachine.State;
import io.openems.edge.goodwe.common.enums.PowerModeEms;

public class EtEmptyHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		if (context.activePowerSetPoint > 0) {
			context.setMode(PowerModeEms.CHARGE_BAT, context.pvProduction - context.activePowerSetPoint);

		} else {
			// Set-Point is negative or zero -> 'charge' from pv production and grid
			context.setMode(PowerModeEms.CHARGE_BAT, context.pvProduction - context.activePowerSetPoint);
		}

		// Evaluate next state
		if (context.soc > 2) {
			return State.ET_DEFAULT;
		} else {
			return State.ET_EMPTY;
		}
	}

}
