package io.openems.edge.batteryinverter.victron.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.batteryinverter.victron.VictronBatteryInverterImpl;
import io.openems.edge.batteryinverter.victron.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class GoStoppedHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		final VictronBatteryInverterImpl inverter = context.getParent();

//		inverter.softStart(false);
//		inverter.setStopInverter();

//		if (inverter.getBatteryInverterState().get() == Boolean.FALSE) {
//			// Inverter is OFF
//			return State.STOPPED;
//		} else {
//			// Still waiting
//			return State.GO_STOPPED;
//		}
		return State.GO_RUNNING;
	}

}
