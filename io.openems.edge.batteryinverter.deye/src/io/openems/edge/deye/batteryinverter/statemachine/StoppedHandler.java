package io.openems.edge.deye.batteryinverter.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.deye.batteryinverter.statemachine.StateMachine.State;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;

public class StoppedHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		final var inverter = context.getParent();

		// Mark as stopped
		inverter._setStartStop(StartStop.STOP);
		return State.STOPPED;
	}

}
