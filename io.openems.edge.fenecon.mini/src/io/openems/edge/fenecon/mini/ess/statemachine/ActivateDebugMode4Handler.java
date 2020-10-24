package io.openems.edge.fenecon.mini.ess.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.fenecon.mini.ess.SetupMode;
import io.openems.edge.fenecon.mini.ess.statemachine.StateMachine.State;

public class ActivateDebugMode4Handler extends StateHandler<State, Context> {

	private final Logger log = LoggerFactory.getLogger(ActivateDebugMode4Handler.class);

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		if (context.component.getSetupMode() != SetupMode.OFF) {
			this.log.info("Wait for Setup-Mode OFF");
			return State.ACTIVATE_DEBUG_MODE_4;
		}

		this.log.info("Setup-Mode is OFF");
		return State.GO_WRITE_MODE;
	}

}
