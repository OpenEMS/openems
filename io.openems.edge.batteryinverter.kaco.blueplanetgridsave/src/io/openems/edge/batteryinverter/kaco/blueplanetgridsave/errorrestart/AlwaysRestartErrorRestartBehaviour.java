package io.openems.edge.batteryinverter.kaco.blueplanetgridsave.errorrestart;

import io.openems.edge.batteryinverter.kaco.blueplanetgridsave.statemachine.Context;
import io.openems.edge.batteryinverter.kaco.blueplanetgridsave.statemachine.StateMachine;

public class AlwaysRestartErrorRestartBehaviour implements ErrorRestartBehaviour {

	@Override
	public StateMachine.State run(Context context) {
		return StateMachine.State.RESTART;
	}

}
