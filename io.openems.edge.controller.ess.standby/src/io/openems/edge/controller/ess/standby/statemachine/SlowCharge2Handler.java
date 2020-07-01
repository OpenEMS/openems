package io.openems.edge.controller.ess.standby.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;

public class SlowCharge2Handler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) throws IllegalArgumentException, OpenemsNamedException {
		SlowChargeUtils.calculateAndApplyPower(context);

		// Evaluate next state
		int allowedChargePower = context.ess.getAllowedChargePower().getOrError();
		if (allowedChargePower == 0) {
			// no more charging allowed
			return State.FINISHED;
		} else {
			// stay in this State
			return State.SLOW_CHARGE_2;
		}
	}

}
