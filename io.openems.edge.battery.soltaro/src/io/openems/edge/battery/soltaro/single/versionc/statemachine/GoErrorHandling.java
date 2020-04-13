package io.openems.edge.battery.soltaro.single.versionc.statemachine;

public class GoErrorHandling extends StateMachine.Handler {

	@Override
	public StateMachine getNextState(StateMachine.Context context) {
		return StateMachine.UNDEFINED;
	}

}
