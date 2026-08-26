package io.openems.edge.controller.ess.emergencycapacityreserve.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.emergencycapacityreserve.statemachine.StateMachine.State;

public class UndefinedHandler extends StateHandler<State, Context> {

	@Override
	protected State runAndGetNextState(Context context) throws OpenemsNamedException {
		if (context.soc != null && context.maxApparentPower != null) {
			var reserveSoc = context.reserveSoc;
			var soc = context.soc;

			if (soc < reserveSoc - 1) {
				return State.FORCE_CHARGE_GRID;
			}

			if (soc == reserveSoc - 1) {
				return State.FORCE_CHARGE_PV;
			}

			if (soc == reserveSoc) {
				return State.AT_RESERVE_SOC;
			}

			if (soc == reserveSoc + 1) {
				return State.ABOVE_RESERVE_SOC;
			}

			return State.NO_LIMIT;
		}

		return State.UNDEFINED;
	}

}
