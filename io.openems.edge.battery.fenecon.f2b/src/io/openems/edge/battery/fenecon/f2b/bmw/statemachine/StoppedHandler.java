package io.openems.edge.battery.fenecon.f2b.bmw.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.fenecon.f2b.bmw.statemachine.StateMachine.State;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;

public class StoppedHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		final var battery = context.getParent();

		if (battery.getStartStopTarget() == StartStop.START) {
			return State.GO_RUNNING;
		}

		// Mark as stopped
		battery._setStartStop(StartStop.STOP);
		return State.STOPPED;
	}

}
