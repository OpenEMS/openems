package io.openems.edge.controller.ess.emergencycapacityreserve.statemachine;

import static io.openems.edge.common.type.TypeUtils.max;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.ess.emergencycapacityreserve.statemachine.StateMachine.State;

public class ForceChargeHandler extends StateHandler<State, Context> {

	@Override
	protected State runAndGetNextState(Context context) throws OpenemsNamedException {
		var sum = context.sum;

		// calculate target and ramp power
		context.setTargetPower(getAcPvProduction(sum) * -1);
		context.setRampPower(context.maxApparentPower * 0.01);

		var reserveSoc = context.reserveSoc;
		int soc = context.soc;

		// SoC is greater or equals then configured reserveSoC
		if (soc >= reserveSoc) {
			return State.AT_RESERVE_SOC;
		}

		return State.FORCE_CHARGE;
	}

	/**
	 * Gets AC-PV Production.
	 * 
	 * @param sum the {@link Sum}
	 * @return the AC-PV Production, always >= 0
	 */
	protected static int getAcPvProduction(Sum sum) {
		return max(sum.getProductionAcActivePower().get(), 0);
	}

}
