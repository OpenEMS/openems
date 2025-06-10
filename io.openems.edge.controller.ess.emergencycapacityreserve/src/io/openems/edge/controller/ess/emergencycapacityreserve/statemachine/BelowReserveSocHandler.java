package io.openems.edge.controller.ess.emergencycapacityreserve.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.emergencycapacityreserve.statemachine.StateMachine.State;

public class BelowReserveSocHandler extends StateHandler<State, Context> {

	@Override
	protected State runAndGetNextState(Context context) throws OpenemsNamedException {
		// calculate target and ramp power
		context.setTargetPower(0);
		context.setRampPower(context.maxApparentPower * 0.05);

		var reserveSoc = context.reserveSoc;
		int soc = context.soc;

		// SoC is atleast 2% under configured reserveSoC and gridcharging is enabled
		if ((soc <= reserveSoc - 2) && context.isEssChargeFromGridAllowed) {
			return State.FORCE_CHARGE_GRID;
		}

		// Enter FORCE_CHARGE_PV sooner when grid charge is allowed
		if (soc <= reserveSoc - 1 || soc <= 0) {
			return State.FORCE_CHARGE_PV;
		}

		// SoC is greater then configured reserveSoC
		if (soc > reserveSoc) {
			return State.AT_RESERVE_SOC;
		}

		return State.BELOW_RESERVE_SOC;
	}

}
