package io.openems.edge.fluidcooling.hydac.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.fluidcooling.hydac.statemachine.StateMachine.State;

public class StoppedHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		final var system = context.getParent();

		if (system.getStartStopTarget() == StartStop.START) {
			return State.RUNNING;
		}
		system.setChillerOff(true);
		return State.STOPPED;

	}

}
