package io.openems.edge.battery.soltaro.single.versionb.statemachine;

import io.openems.edge.battery.soltaro.single.versionb.statemachine.StateMachine.State;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;

public class RunningHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		if (ControlAndLogic.hasError(context.component, context.config.numberOfSlaves())) {
			return State.UNDEFINED;
		}

		if (!ControlAndLogic.isSystemRunning(context.component)) {
			return State.UNDEFINED;
		}

		// Mark as started
		context.component._setStartStop(StartStop.START);

		return State.RUNNING;
	}

}
