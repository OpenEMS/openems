package io.openems.edge.ess.generic.symmetric.statemachine;

import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.ess.generic.symmetric.statemachine.StateMachine.State;

public class MaintenanceHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		if (context.battery.isFirmwareUpdateRunning()) {
			return State.MAINTENANCE;
		} else {
			return State.UNDEFINED;
		}
	}
}
