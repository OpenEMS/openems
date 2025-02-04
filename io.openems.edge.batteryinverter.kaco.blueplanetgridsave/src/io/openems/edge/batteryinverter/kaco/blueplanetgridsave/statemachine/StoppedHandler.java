package io.openems.edge.batteryinverter.kaco.blueplanetgridsave.statemachine;

import io.openems.edge.batteryinverter.kaco.blueplanetgridsave.statemachine.StateMachine.State;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;

public class StoppedHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		final var inverter = context.getParent();

		if (inverter.hasFaults()) {
			return State.ERROR;
		}

		inverter._setStartStop(StartStop.STOP);
		return State.STOPPED;
	}
}
