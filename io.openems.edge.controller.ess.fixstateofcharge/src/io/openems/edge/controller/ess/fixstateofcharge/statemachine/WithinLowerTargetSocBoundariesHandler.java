package io.openems.edge.controller.ess.fixstateofcharge.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.fixstateofcharge.statemachine.StateMachine.State;

public class WithinLowerTargetSocBoundariesHandler extends StateHandler<State, Context> {

	@Override
	protected State runAndGetNextState(Context context) throws OpenemsNamedException {

		/*
		 * Decrease the charge/discharge power when the target state of charge is almost
		 * reached.
		 */
		context.setTargetPower(context.getBoundariesPower() * -1); // min
		context.setRampPower(context.maxApparentPower * 0.05);

		return Context.getSocState(context.soc, context.targetSoc);
	}
}
