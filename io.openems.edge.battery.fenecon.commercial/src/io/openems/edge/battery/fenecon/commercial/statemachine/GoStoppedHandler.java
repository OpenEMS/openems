package io.openems.edge.battery.fenecon.commercial.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.fenecon.commercial.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class GoStoppedHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		// Open the relay
		context.setBatteryStartUpRelays(true);
		if (context.getBatteryStartStopRelay() == Boolean.TRUE) {
			return State.STOPPED;
		}
		return State.GO_STOPPED;
	}
}
