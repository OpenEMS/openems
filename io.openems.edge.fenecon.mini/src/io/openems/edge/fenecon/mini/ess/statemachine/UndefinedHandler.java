package io.openems.edge.fenecon.mini.ess.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.fenecon.mini.ess.PcsMode;
import io.openems.edge.fenecon.mini.ess.SetupMode;
import io.openems.edge.fenecon.mini.ess.statemachine.StateMachine.State;

public class UndefinedHandler extends StateHandler<State, Context> {

	private final Logger log = LoggerFactory.getLogger(UndefinedHandler.class);

	@Override
	public State runAndGetNextState(Context context) {
		var ess = context.getParent();

		if (ess.getPcsMode() == PcsMode.UNDEFINED) {
			context.logInfo(this.log, "Wait for PCS Mode to be defined");
			return State.UNDEFINED;
		}
		if (ess.getSetupMode() == SetupMode.UNDEFINED) {
			context.logInfo(this.log, "Wait for Setup-Mode to be defined");
			return State.UNDEFINED;
		}

		if (context.config.readonly()) {
			return State.GO_READONLY_MODE;
		}
		return State.GO_WRITE_MODE;
	}

}
