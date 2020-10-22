package io.openems.edge.goodwe.et.ess.applypower;

import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.goodwe.et.ess.PowerModeEms;
import io.openems.edge.goodwe.et.ess.applypower.ApplyPowerStateMachine.State;

public class FullNegativeCurtailHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		
		context.setMode(PowerModeEms.EXPORT_AC, 0);

		return State.FULL_NEGATIVE_CURTAIL;
	}

}
