package io.openems.edge.goodwe.common.applypower;

import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.goodwe.common.applypower.ApplyPowerStateMachine.State;
import io.openems.edge.goodwe.common.enums.PowerModeEms;

public class EtFullHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		if (context.activePowerSetPoint > 0) {
			// Set-Point is positive -> take power either from pv or battery
			if (context.activePowerSetPoint > context.pvProduction) {
				context.setMode(PowerModeEms.DISCHARGE_BAT, context.activePowerSetPoint - context.pvProduction);
			} else {
				context.setMode(PowerModeEms.EXPORT_AC, context.activePowerSetPoint);
			}
		} else {
			// Set-Point is negative or zero
			context.setMode(PowerModeEms.EXPORT_AC, 0);
		}

		// Evaluate next state
		if (context.soc < 98) {
			return State.ET_DEFAULT;
		} else {
			return State.ET_FULL;
		}
	}

}
