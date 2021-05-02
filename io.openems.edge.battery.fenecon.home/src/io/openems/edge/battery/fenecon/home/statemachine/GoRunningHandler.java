package io.openems.edge.battery.fenecon.home.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.fenecon.home.FeneconHomeBattery;
import io.openems.edge.battery.fenecon.home.enums.BmsControl;
import io.openems.edge.battery.fenecon.home.statemachine.StateMachine.State;
import io.openems.edge.common.statemachine.StateHandler;

public class GoRunningHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		FeneconHomeBattery battery = context.getParent();
		BmsControl bmsControl = battery.getBmsControl();

		// We can no nothing but wait...
		if (bmsControl == BmsControl.SWITCHED_ON || bmsControl ==  BmsControl.IGNORED) {
			return State.RUNNING;

		} else {
			// Still waiting...
			return State.GO_RUNNING;
		}
	}

}
