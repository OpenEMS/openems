package io.openems.edge.battery.fenecon.home.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.battery.fenecon.home.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class GoStoppedHandler extends StateHandler<State, Context> {

	private final Logger log = LoggerFactory.getLogger(GoStoppedHandler.class);

	@Override
	public State runAndGetNextState(Context context) {
		context.logWarn(this.log, "Stopping a FENECON Home Battery is not supported");
		return State.GO_STOPPED;
	}

}
