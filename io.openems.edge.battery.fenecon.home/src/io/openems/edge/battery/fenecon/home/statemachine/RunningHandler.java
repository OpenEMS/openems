package io.openems.edge.battery.fenecon.home.statemachine;

import io.openems.edge.battery.fenecon.home.FeneconHomeBattery;
import io.openems.edge.battery.fenecon.home.enums.BmsControl;
import io.openems.edge.battery.fenecon.home.statemachine.StateMachine.State;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;

public class RunningHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		FeneconHomeBattery battery = context.getParent();

		if (battery.hasFaults()) {
			return State.UNDEFINED;
		}

		if (battery.getBmsControl() != BmsControl.SWITCHED_ON
				&& battery.getBmsControl() != BmsControl.IGNORED) {
			return State.UNDEFINED;
		}

		// Mark as started
		battery._setStartStop(StartStop.START);

		return State.RUNNING;
	}

}
