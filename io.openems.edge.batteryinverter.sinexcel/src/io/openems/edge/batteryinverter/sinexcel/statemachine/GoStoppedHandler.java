package io.openems.edge.batteryinverter.sinexcel.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.batteryinverter.sinexcel.SinexcelImpl;
import io.openems.edge.batteryinverter.sinexcel.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class GoStoppedHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		final SinexcelImpl inverter = context.getParent();

		inverter.softStart(false);
		inverter.setStopCommand();

		if (inverter.getBatteryInverterState().get() == Boolean.FALSE) {
			// Inverter is OFF
			return State.STOPPED;
		} else {
			// Still waiting
			return State.GO_STOPPED;
		}
	}

}
