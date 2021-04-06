package io.openems.edge.goodwe.common.applypower;

import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.goodwe.common.applypower.ApplyPowerStateMachine.State;
import io.openems.edge.goodwe.common.enums.PowerModeEms;

public class UndefinedHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		context.setMode(PowerModeEms.AUTO, 0);

		switch (context.goodweType) {
		case GOODWE_5K_BT:
		case GOODWE_8K_BT:
		case GOODWE_10K_BT:
			return State.BT;

		case GOODWE_5K_ET:
		case GOODWE_8K_ET:
		case GOODWE_10K_ET:
			return State.ET_DEFAULT;

		case UNDEFINED:
			return State.UNDEFINED;
		}
		// will never reach here
		return State.UNDEFINED;
	}

}
