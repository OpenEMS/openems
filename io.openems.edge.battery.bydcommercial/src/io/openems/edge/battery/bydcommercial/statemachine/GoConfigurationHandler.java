package io.openems.edge.battery.bydcommercial.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.bydcommercial.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class GoConfigurationHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		System.out.println("Stuck in GO_CONFIGURATION");
		return State.GO_CONFIGURATION;
	}

}
