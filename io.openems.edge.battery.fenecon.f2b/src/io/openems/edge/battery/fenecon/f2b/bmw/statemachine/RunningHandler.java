package io.openems.edge.battery.fenecon.f2b.bmw.statemachine;

import io.openems.edge.battery.fenecon.f2b.bmw.enums.HvContactorStatus;
import io.openems.edge.battery.fenecon.f2b.bmw.statemachine.StateMachine.State;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;

public class RunningHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		final var battery = context.getParent();
		if (battery.hasFaults()) {
			return State.ERROR;
		}

		if (battery.getHvContactorStatus().isDefined()
				&& battery.getHvContactorStatus().asEnum() != HvContactorStatus.CONTACTORS_CLOSED) {
			battery._setHvContactorsOpenedInRunning(true);
		}

		// If its not null,a critical error has occurred.
		if (context.isAnyActiveCatError()) {
			return State.ERROR;
		}

		if (battery.getStartStopTarget() == StartStop.STOP) {
			return State.GO_STOPPED;
		}
		// Mark as started
		battery._setStartStop(StartStop.START);
		return State.RUNNING;
	}
}
