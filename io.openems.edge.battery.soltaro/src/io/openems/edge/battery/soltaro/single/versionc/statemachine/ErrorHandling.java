package io.openems.edge.battery.soltaro.single.versionc.statemachine;

public class ErrorHandling extends StateMachine.Handler {

	@Override
	public StateMachine getNextState(StateMachine.Context context) {
		return StateMachine.ERROR_HANDLING;
	}

}
