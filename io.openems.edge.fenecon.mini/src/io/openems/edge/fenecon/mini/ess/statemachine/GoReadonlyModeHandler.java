package io.openems.edge.fenecon.mini.ess.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.fenecon.mini.ess.SetupMode;
import io.openems.edge.fenecon.mini.ess.statemachine.StateMachine.State;

public class GoReadonlyModeHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		if (context.getParent().getSetupMode() == SetupMode.OFF) {
			switch (context.getParent().getPcsMode()) {
			case CONSUMERS_PEAK_PATTERN:
			case ECO:
			case ECONOMIC:
			case EMERGENCY:
			case RIYUAN:
			case SMOOTH_PV:
			case TIMING:
				// Not in setup-mode and PCS-Mode is valid
				return State.READONLY_MODE;
			case REMOTE:
			case DEBUG:
			case UNDEFINED:
				break;
			}
		}

		// Every other state
		return State.ACTIVATE_ECONOMIC_MODE_1;
	}

}
