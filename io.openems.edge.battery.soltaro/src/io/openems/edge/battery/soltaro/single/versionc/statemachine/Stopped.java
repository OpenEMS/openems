package io.openems.edge.battery.soltaro.single.versionc.statemachine;

import io.openems.edge.battery.soltaro.single.versionc.statemachine.StateMachine.Context;

public class Stopped extends State.Handler {

	@Override
	public State getNextState(Context context) {
		return State.UNDEFINED;
	}

}
