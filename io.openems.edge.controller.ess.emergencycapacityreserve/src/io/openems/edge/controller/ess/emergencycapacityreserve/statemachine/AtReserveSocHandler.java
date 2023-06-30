package io.openems.edge.controller.ess.emergencycapacityreserve.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.emergencycapacityreserve.statemachine.StateMachine.State;

public class AtReserveSocHandler extends StateHandler<State, Context> {

	@Override
	protected State runAndGetNextState(Context context) throws OpenemsNamedException {
		var sum = context.sum;

		// calculate target and ramp power
		var targetPower = Math.max(0, sum.getProductionDcActualPower().orElse(0));
		context.setTargetPower(targetPower);
		context.setRampPower(context.maxApparentPower * 0.01);

		var reserveSoc = context.reserveSoc;
		int soc = context.soc;

		// SoC is under configured reserveSoC
		if (soc < reserveSoc) {
			return State.BELOW_RESERVE_SOC;
		}

		// SoC is under configured reserveSoC
		if (soc > reserveSoc) {
			return State.ABOVE_RESERVE_SOC;
		}

		return State.AT_RESERVE_SOC;
	}

}
