package io.openems.edge.battery.soltaro.single.versionc.statemachine;

public class Undefined extends StateMachine.Handler {

	@Override
	public StateMachine getNextState(StateMachine.Context context) {
		if (context.config.switchedOn()) {
			// Switched ON
			if (context.faults.isEmpty()) {
				// No Faults -> start
				return StateMachine.GO_RUNNING;
			} else {
				// Has Faults -> error handling
				return StateMachine.GO_ERROR_HANDLING;
			}
		} else {
			// Switched OFF
			return StateMachine.GO_STOPPED;
		}
	}
}
