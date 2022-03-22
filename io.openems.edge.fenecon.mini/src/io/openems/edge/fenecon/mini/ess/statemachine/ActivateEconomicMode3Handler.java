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
		var ess = context.getParent();

		if (ess.getPcsMode() != PcsMode.ECONOMIC) {
			context.logInfo(this.log, "Wait for PCS-Mode ECONOMIC");
			return State.ACTIVATE_ECONOMIC_MODE_3;
		}

		context.logInfo(this.log, "PCS-Mode is ECONOMIC -> Set Setup-Mode OFF");
		ess.setSetupMode(SetupMode.OFF);

		return State.ACTIVATE_ECONOMIC_MODE_4;
	}

}
