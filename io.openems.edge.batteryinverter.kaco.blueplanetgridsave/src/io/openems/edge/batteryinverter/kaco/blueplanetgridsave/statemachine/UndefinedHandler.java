package io.openems.edge.batteryinverter.kaco.blueplanetgridsave.statemachine;

import io.openems.edge.batteryinverter.kaco.blueplanetgridsave.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class UndefinedHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		final var inverter = context.getParent();

		if (inverter.getCurrentState().isUndefined()) {
			return State.UNDEFINED;
		}

		if (inverter.hasFailure()) {
			return State.ERROR;
		}

		if (inverter.isRunning()) {
			return State.RUNNING;
		}

		if (inverter.isShutdown()) {
			return State.STOPPED;
		}

		return State.GO_STOPPED;
	}
}
