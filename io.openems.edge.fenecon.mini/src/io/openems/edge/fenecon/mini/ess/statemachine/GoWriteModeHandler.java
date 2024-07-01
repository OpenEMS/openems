package io.openems.edge.fenecon.mini.ess.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.fenecon.mini.ess.PcsMode;
import io.openems.edge.fenecon.mini.ess.SetupMode;
import io.openems.edge.fenecon.mini.ess.statemachine.StateMachine.State;

public class GoWriteModeHandler extends StateHandler<State, Context> {

	private final Logger log = LoggerFactory.getLogger(ActivateDebugMode1Handler.class);

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		var ess = context.getParent();

		if (ess.getPcsMode() == PcsMode.UNDEFINED) {
			context.logInfo(this.log, "Wait for PCS Mode to be defined");
			return State.GO_WRITE_MODE;
		}
		if (ess.getSetupMode() == SetupMode.UNDEFINED) {
			context.logInfo(this.log, "Wait for Setup-Mode to be defined");
			return State.GO_WRITE_MODE;
		}

		if (ess.getPcsMode() == PcsMode.DEBUG && ess.getSetupMode() == SetupMode.OFF) {
			return State.WRITE_MODE;
		}

		return State.ACTIVATE_DEBUG_MODE_1;
	}

}
