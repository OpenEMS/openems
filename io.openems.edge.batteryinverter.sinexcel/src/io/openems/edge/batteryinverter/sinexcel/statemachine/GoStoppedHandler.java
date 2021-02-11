package io.openems.edge.batteryinverter.sinexcel.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.batteryinverter.sinexcel.Sinexcel;
import io.openems.edge.batteryinverter.sinexcel.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class GoStoppedHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		Sinexcel inverter = context.getParent();

		// Has Faults -> abort
		if (inverter.hasFaults()) {
			return State.UNDEFINED;
		}

		context.softStart(false);
		context.setInverterOff();

		if (inverter.getStateOn().orElse(true)) {
			// Still waiting
			return State.GO_STOPPED;
		} else {
			// Inverter is OFF
			return State.STOPPED;
		}
	}

}
