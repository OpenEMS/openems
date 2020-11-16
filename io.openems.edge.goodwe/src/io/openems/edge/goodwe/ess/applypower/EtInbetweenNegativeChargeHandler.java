package io.openems.edge.goodwe.ess.applypower;

import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.goodwe.ess.applypower.ApplyPowerStateMachine.State;
import io.openems.edge.goodwe.ess.enums.PowerModeEms;

public class EtInbetweenNegativeChargeHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {

		context.setMode(PowerModeEms.CHARGE_BAT, context.pvProduction - context.activePowerSetPoint);

		return State.ET_INBETWEEN_NEGATIVE_CHARGE;
	}

}
