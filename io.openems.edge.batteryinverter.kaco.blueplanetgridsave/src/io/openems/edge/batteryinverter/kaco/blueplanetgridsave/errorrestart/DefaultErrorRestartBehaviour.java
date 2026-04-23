package io.openems.edge.batteryinverter.kaco.blueplanetgridsave.errorrestart;

import io.openems.edge.batteryinverter.kaco.blueplanetgridsave.statemachine.Context;
import io.openems.edge.batteryinverter.kaco.blueplanetgridsave.statemachine.StateMachine;

public class DefaultErrorRestartBehaviour implements ErrorRestartBehaviour {

	@Override
	public StateMachine.State run(Context context) {
		final var inverter = context.getParent();

		if (!inverter.hasFailure()) {
			return StateMachine.State.UNDEFINED;
		}

		return StateMachine.State.ERROR;
	}

}
