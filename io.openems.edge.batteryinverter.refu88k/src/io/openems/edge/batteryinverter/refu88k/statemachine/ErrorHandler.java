package io.openems.edge.batteryinverter.refu88k.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.batteryinverter.refu88k.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class ErrorHandler extends StateHandler<State, Context> {

	@Override
	public StateMachine.State runAndGetNextState(Context context) throws OpenemsNamedException {
		var inverter = context.getParent();

		inverter._setActivePower(0);
		inverter._setReactivePower(0);

		// Try again
		return State.UNDEFINED;

	}

}
