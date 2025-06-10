package io.openems.edge.ess.generic.offgrid.statemachine;

import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.ess.generic.offgrid.statemachine.StateMachine.OffGridState;

public class StoppedHandler extends StateHandler<OffGridState, Context> {

	@Override
	public OffGridState runAndGetNextState(Context context) {
		final var ess = context.getParent();

		// Mark as stopped
		ess._setStartStop(StartStop.STOP);

		return OffGridState.STOPPED;
	}

}
