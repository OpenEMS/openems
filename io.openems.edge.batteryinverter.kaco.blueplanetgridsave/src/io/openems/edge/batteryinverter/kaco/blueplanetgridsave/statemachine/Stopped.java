package io.openems.edge.batteryinverter.kaco.blueplanetgridsave.statemachine;

import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;

public class Stopped extends StateHandler<State, Context> {

	@Override
	public State getNextState(Context context) {
		// Mark as stopped
		context.component._setStartStop(StartStop.STOP);

		return State.UNDEFINED;
	}

}
