package io.openems.edge.battery.pylontech.powercubem2.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.battery.pylontech.powercubem2.statemachine.StateMachine.State;
import io.openems.edge.common.startstop.StartStop;

public class StoppedHandler extends StateHandler<State, Context> {


	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {

		// Mark as stopped
		var battery = context.getParent();
		battery._setStartStop(StartStop.STOP);

		return State.STOPPED;
	}


}