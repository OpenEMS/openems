package io.openems.edge.fenecon.mini.ess.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.fenecon.mini.ess.FeneconMiniEss;
import io.openems.edge.fenecon.mini.ess.PcsMode;
import io.openems.edge.fenecon.mini.ess.SetupMode;
import io.openems.edge.fenecon.mini.ess.statemachine.StateMachine.State;

public class GoReadonlyModeHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		FeneconMiniEss ess = context.getParent();

		if (ess.getPcsMode() == PcsMode.ECONOMIC && ess.getSetupMode() == SetupMode.OFF) {
			return State.READONLY_MODE;
		}

		return State.ACTIVATE_ECONOMIC_MODE_1;
	}

}
