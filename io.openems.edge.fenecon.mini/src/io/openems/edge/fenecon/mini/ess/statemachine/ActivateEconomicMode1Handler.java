package io.openems.edge.fenecon.mini.ess.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.fenecon.mini.ess.SetupMode;
import io.openems.edge.fenecon.mini.ess.statemachine.StateMachine.State;

public class ActivateEconomicMode1Handler extends StateHandler<State, Context> {

	private final Logger log = LoggerFactory.getLogger(ActivateEconomicMode1Handler.class);

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		var ess = context.getParent();

		if (ess.getSetupMode() != SetupMode.ON) {
			context.logInfo(this.log, "Activate Debug-Mode: Set Setup-Mode ON");
			ess.setSetupMode(SetupMode.ON);
		}

		return State.ACTIVATE_ECONOMIC_MODE_2;
	}

}
