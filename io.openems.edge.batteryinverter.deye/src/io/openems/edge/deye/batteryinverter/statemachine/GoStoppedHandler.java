package io.openems.edge.deye.batteryinverter.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.deye.batteryinverter.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class GoStoppedHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		final var inverter = context.getParent();

		inverter.setStopInverter();

		if (inverter.getInverterState().get() == Boolean.FALSE) {
			// Inverter is OFF
			return State.STOPPED;
		}
		// Still waiting
		return State.GO_STOPPED;
	}

}
