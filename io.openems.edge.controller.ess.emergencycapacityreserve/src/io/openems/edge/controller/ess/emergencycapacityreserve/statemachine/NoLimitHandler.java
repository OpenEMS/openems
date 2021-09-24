package io.openems.edge.controller.ess.emergencycapacityreserve.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.emergencycapacityreserve.statemachine.StateMachine.State;

public class NoLimitHandler extends StateHandler<State, Context> {

	@Override
	protected State runAndGetNextState(Context context) throws OpenemsNamedException {
		// set target power
		context.setTargetPower(context.maxApparentPower);

		int reserveSoc = context.reserveSoc;
		Integer soc = context.soc;

		// SoC is just above reserveSoC
		if (soc != null && soc <= reserveSoc + 1) {
			return State.ABOVE_RESERVE_SOC;
		}

		return State.NO_LIMIT;
	}

}
