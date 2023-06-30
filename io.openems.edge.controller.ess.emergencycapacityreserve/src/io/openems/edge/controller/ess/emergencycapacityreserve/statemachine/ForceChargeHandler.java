package io.openems.edge.controller.ess.emergencycapacityreserve.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.emergencycapacityreserve.statemachine.StateMachine.State;

public class ForceChargeHandler extends StateHandler<State, Context> {

	@Override
	protected State runAndGetNextState(Context context) throws OpenemsNamedException {
		var sum = context.sum;

		// calculate target and ramp power
		int acProduction = sum.getProductionAcActivePower().orElse(0);
		context.setTargetPower(acProduction * -1);
		context.setRampPower(context.maxApparentPower * 0.01);

		var reserveSoc = context.reserveSoc;
		int soc = context.soc;

		// SoC is greater or equals then configured reserveSoC
		if (soc >= reserveSoc) {
			return State.AT_RESERVE_SOC;
		}

		return State.FORCE_CHARGE;
	}

}
