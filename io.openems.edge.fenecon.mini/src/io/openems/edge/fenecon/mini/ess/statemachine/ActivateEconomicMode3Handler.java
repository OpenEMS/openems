package io.openems.edge.fenecon.mini.ess.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.fenecon.mini.ess.PcsMode;
import io.openems.edge.fenecon.mini.ess.SetupMode;
import io.openems.edge.fenecon.mini.ess.statemachine.StateMachine.State;

public class ActivateEconomicMode3Handler extends StateHandler<State, Context> {

	private final Logger log = LoggerFactory.getLogger(ActivateEconomicMode3Handler.class);

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		if (context.component.getPcsMode() != PcsMode.ECONOMIC) {
			this.log.info("Wait for PCS-Mode ECONOMIC");
			return State.ACTIVATE_ECONOMIC_MODE_3;
		}

		this.log.info("PCS-Mode is ECONOMIC -> Set Setup-Mode OFF");
		context.component.setSetupMode(SetupMode.OFF);

		return State.ACTIVATE_ECONOMIC_MODE_4;
	}

}
