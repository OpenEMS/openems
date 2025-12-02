package io.openems.edge.batteryinverter.victron.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.batteryinverter.victron.statemachine.StateMachine.State;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;

/**
 * Handles the GO_STOPPED state - transition from running to stopped.
 */
public class GoStoppedHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		final var inverter = context.getParent();

		// Disable soft start
		inverter.softStart(false);

		// Mark as stopped
		inverter._setStartStop(StartStop.STOP);

		// Victron doesn't have explicit stop command - transition directly to STOPPED
		return State.STOPPED;
	}

}
