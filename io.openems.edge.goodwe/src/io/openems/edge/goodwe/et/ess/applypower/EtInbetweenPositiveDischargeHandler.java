package io.openems.edge.goodwe.et.ess.applypower;

import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.goodwe.et.ess.applypower.ApplyPowerStateMachine.State;
import io.openems.edge.goodwe.et.ess.enums.PowerModeEms;

public class EtInbetweenPositiveDischargeHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		context.setMode(PowerModeEms.DISCHARGE_PV, context.activePowerSetPoint - context.pvProduction);

		return State.ET_INBETWEEN_POSITIVE_DISCHARGE;
	}

}
