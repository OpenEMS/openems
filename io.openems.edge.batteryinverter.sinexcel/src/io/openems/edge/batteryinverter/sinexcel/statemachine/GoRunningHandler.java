package io.openems.edge.batteryinverter.sinexcel.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.batteryinverter.sinexcel.SinexcelImpl;
import io.openems.edge.batteryinverter.sinexcel.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class GoRunningHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		final SinexcelImpl inverter = context.getParent();

		/*
		 * Be sure to set the correct target grid mode
		 */
		Boolean setOnGridMode = inverter.getSetOnGridMode().get();
		Boolean setOffGridMode = inverter.getSetOffGridMode().get();
		switch (context.targetGridMode) {
		case GO_ON_GRID:
			if (setOnGridMode == Boolean.FALSE || setOffGridMode == Boolean.TRUE) {
				inverter.setOnGridMode(true);
				inverter.setOffGridMode(false);
				return State.GO_RUNNING;
			}
			break;
		case GO_OFF_GRID:
			if (setOnGridMode == Boolean.TRUE || setOffGridMode == Boolean.FALSE) {
				inverter.setOnGridMode(false);
				inverter.setOffGridMode(true);
				return State.GO_RUNNING;
			}
			break;
		}

		inverter.softStart(true);
		inverter.setStartInverter();

		if (inverter.getBatteryInverterState().get() == Boolean.TRUE) {
			// Inverter is ON
			return State.RUNNING;
		} else {
			// Still waiting
			return State.GO_RUNNING;
		}
	}

}
