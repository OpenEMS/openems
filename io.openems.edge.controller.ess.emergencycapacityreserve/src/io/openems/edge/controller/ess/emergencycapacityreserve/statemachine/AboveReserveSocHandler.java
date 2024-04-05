package io.openems.edge.controller.ess.emergencycapacityreserve.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.emergencycapacityreserve.statemachine.StateMachine.State;

public class AboveReserveSocHandler extends StateHandler<State, Context> {

	@Override
	protected State runAndGetNextState(Context context) throws OpenemsNamedException {
		var sum = context.sum;

		// calculate target and ramp power
		var targetPower = Math.max(context.maxApparentPower / 2, sum.getProductionDcActualPower().orElse(0));
		context.setTargetPower(targetPower);
		context.setRampPower(context.maxApparentPower * 0.01);

		var reserveSoc = context.reserveSoc;
		int soc = context.soc;

		// SoC is less or equals then configured reserveSoC
		if (soc <= reserveSoc) {
			return State.AT_RESERVE_SOC;
		}

		// SoC is greater then configured reserveSoC
		if (soc > reserveSoc + 1) {
			return State.NO_LIMIT;
		}

		return State.ABOVE_RESERVE_SOC;
	}

}
