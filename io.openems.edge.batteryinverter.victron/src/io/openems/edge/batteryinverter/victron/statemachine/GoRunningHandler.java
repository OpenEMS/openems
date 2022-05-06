package io.openems.edge.batteryinverter.victron.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.batteryinverter.victron.VictronBatteryInverterImpl;
import io.openems.edge.batteryinverter.victron.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.common.sum.GridMode;

public class GoRunningHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		final VictronBatteryInverterImpl inverter = context.getParent();

		//TODO
		inverter._setGridMode(GridMode.OFF_GRID);
//		/*
//		 * Be sure to set the correct target grid mode
//		 */
//		Boolean setOnGridMode = inverter.getSetOnGridMode().get();
//		Boolean setOffGridMode = inverter.getSetOffGridMode().get();
//		switch (context.targetGridMode) {
//		case GO_ON_GRID:
//			if (setOnGridMode == Boolean.FALSE || setOffGridMode == Boolean.TRUE) {
//				inverter.setOnGridMode(true);
//				inverter.setOffGridMode(false);
//				return State.GO_RUNNING;
//			}
//			break;
//		case GO_OFF_GRID:
//			if (setOnGridMode == Boolean.TRUE || setOffGridMode == Boolean.FALSE) {
//				inverter.setOnGridMode(false);
//				inverter.setOffGridMode(true);
//				return State.GO_RUNNING;
//			}
//			break;
//		}

//		inverter.softStart(true);
//		inverter.setStartInverter();

//		if (inverter.getBatteryInverterState().get() == Boolean.TRUE) {
			// Inverter is ON
			return State.RUNNING;
//		} else {
//			// Still waiting
//			return State.GO_RUNNING;
//		}
	}

}
