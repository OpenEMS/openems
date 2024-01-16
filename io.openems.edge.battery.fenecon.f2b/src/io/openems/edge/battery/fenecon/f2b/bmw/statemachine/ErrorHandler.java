package io.openems.edge.battery.fenecon.f2b.bmw.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.fenecon.f2b.bmw.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class ErrorHandler extends StateHandler<State, Context> {

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		var battery = context.getParent();
		battery.setHvContactor(false);
		battery.stop();
	}

	@Override
	public State runAndGetNextState(Context context) {
		return State.ERROR;
	}
}
