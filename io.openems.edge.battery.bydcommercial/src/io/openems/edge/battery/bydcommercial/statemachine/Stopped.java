package io.openems.edge.battery.bydcommercial.statemachine;

import io.openems.edge.battery.bydcommercial.statemachine.StateMachine.Context;

public class Stopped extends State.Handler {

	@Override
	public State getNextState(Context context) {
		return State.UNDEFINED;
	}

}
