package io.openems.edge.battery.fenecon.f2b.dummy.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.fenecon.f2b.dummy.statemachine.StateMachine.State;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;

public class RunningHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		final var battery = context.getParent();

		// Has Faults -> error handling
		if (battery.hasFaults()) {
			return State.ERROR;
		}

		if (battery.getStartStopTarget() == StartStop.STOP) {
			return State.GO_STOPPED;
		}

		battery._setStartStop(StartStop.START);
		return State.RUNNING;
	}
}
