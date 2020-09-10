package io.openems.edge.battery.bmw.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.bmw.statemachine.StateMachine.State;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;

public class RunningHandler extends StateHandler<State, Context> {

	@Override
	protected State runAndGetNextState(Context context) throws OpenemsNamedException {
		if (context.component.hasFaults()) {
			return State.UNDEFINED;
		}
		
		if (!context.component.isSystemRunning()) {
			return State.UNDEFINED;
		}

		context.component._setStartStop(StartStop.START);

		return State.RUNNING;
	}

}
