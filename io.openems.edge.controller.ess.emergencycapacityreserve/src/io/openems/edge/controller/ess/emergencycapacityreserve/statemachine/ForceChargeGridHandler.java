package io.openems.edge.controller.ess.emergencycapacityreserve.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.emergencycapacityreserve.statemachine.StateMachine.State;

public class ForceChargeGridHandler extends StateHandler<State, Context> {

	@Override
	protected State runAndGetNextState(Context context) throws OpenemsNamedException {
		var reserveSoc = context.reserveSoc;
		int soc = context.soc;

		// leave grid charge logic when grid charge gets disabled
		if (!context.isEssChargeFromGridAllowed) {
			if (soc <= reserveSoc - 1) {
				return State.BELOW_RESERVE_SOC;
			}
			return State.AT_RESERVE_SOC;
		}

		float targetPower;

		if (soc <= reserveSoc - 4 || soc <= 0) {
			targetPower = context.maxApparentPower * -0.5f;
		} else {
			targetPower = context.maxApparentPower * -0.1f;
		}

		// calculate target and ramp power
		context.setTargetPower(targetPower);
		context.setRampPower(context.maxApparentPower * 0.01);

		// SoC is greater or equals then configured reserveSoC or 100
		if (soc >= reserveSoc + 1 || soc == 100) {
			return State.AT_RESERVE_SOC;
		}

		return State.FORCE_CHARGE_GRID;
	}

}
