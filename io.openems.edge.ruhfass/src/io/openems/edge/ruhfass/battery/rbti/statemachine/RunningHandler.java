package io.openems.edge.ruhfass.battery.rbti.statemachine;

import io.openems.common.timedata.Timeout;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.ruhfass.battery.rbti.statemachine.StateMachine.State;

public class RunningHandler extends StateHandler<State, Context> {

	private final Timeout timeout = Timeout.ofSeconds(5);

	@Override
	protected void onEntry(Context context) {
		this.timeout.start(context.clock);
	}

	@Override
	public State runAndGetNextState(Context context) {
		final var battery = context.getParent();
		if (battery.hasFaults()) {
			return State.ERROR;
		}

		battery._setStartStop(StartStop.START);
		return State.RUNNING;
	}
}