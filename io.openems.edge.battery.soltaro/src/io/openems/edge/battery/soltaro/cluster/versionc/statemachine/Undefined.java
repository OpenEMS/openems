package io.openems.edge.battery.soltaro.cluster.versionc.statemachine;

import io.openems.edge.battery.soltaro.cluster.versionc.statemachine.StateMachine.Context;

public class Undefined extends State.Handler {

	@Override
	public State getNextState(Context context) {
		if (context.config.switchedOn()) {
			// Switched ON
			if (context.component.hasFaults()) {
				// Has Faults -> error handling
				return State.ERROR_HANDLING;
			} else {
				// No Faults -> start
				return State.GO_RUNNING;
			}
		} else {
			// Switched OFF
			return State.GO_STOPPED;
		}
	}

}
