package io.openems.edge.fenecon.mini.ess.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.fenecon.mini.ess.PcsMode;
import io.openems.edge.fenecon.mini.ess.SetupMode;
import io.openems.edge.fenecon.mini.ess.statemachine.StateMachine.State;

public class ActivateDebugMode2Handler extends StateHandler<State, Context> {

	private final Logger log = LoggerFactory.getLogger(ActivateDebugMode2Handler.class);

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		if (context.component.getSetupMode() != SetupMode.ON) {
			this.log.info("Wait for Setup-Mode ON");
			return State.ACTIVATE_DEBUG_MODE_2;
		}

		this.log.info("Setup-Mode is ON -> Set PCS-Mode DEBUG");
		context.component.setPcsMode(PcsMode.DEBUG);
		return State.ACTIVATE_DEBUG_MODE_3;
	}

}
