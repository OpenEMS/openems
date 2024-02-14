package io.openems.edge.deye.batteryinverter.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.deye.batteryinverter.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class GoRunningHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		final var inverter = context.getParent();

		/*
		 * Be sure to set the correct target grid mode
		 */
		var setOnGridMode = inverter.getSetOnGridMode().get();
		var setOffGridMode = inverter.getSetOffGridMode().get();
		switch (context.targetGridMode) {
		case GO_ON_GRID:
			if (setOnGridMode == Boolean.FALSE || setOffGridMode == Boolean.TRUE) {
				inverter.setOnGridMode(true);
				return State.GO_RUNNING;
			}
			break;
		case GO_OFF_GRID:
			if (setOnGridMode == Boolean.TRUE || setOffGridMode == Boolean.FALSE) {
				inverter.setOffGridMode(true);
				return State.GO_RUNNING;
			}
			break;
		}

		inverter.setStartInverter();

		if (inverter.getInverterState().get() == Boolean.TRUE) {
			// Inverter is ON
			return State.RUNNING;
		}
		// Still waiting
		return State.GO_RUNNING;
	}

}
