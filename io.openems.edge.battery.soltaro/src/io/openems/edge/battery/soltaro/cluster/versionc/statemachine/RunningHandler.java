package io.openems.edge.battery.soltaro.cluster.versionc.statemachine;

import io.openems.edge.battery.soltaro.cluster.versionc.statemachine.StateMachine.State;
import io.openems.edge.battery.soltaro.single.versionc.enums.PreChargeControl;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;

public class RunningHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		if (context.component.hasFaults()) {
			return State.UNDEFINED;
		}

		PreChargeControl commonPreChargeControl = context.component.getCommonPreChargeControl()
				.orElse(PreChargeControl.UNDEFINED);
		if (commonPreChargeControl != PreChargeControl.RUNNING) {
			return State.UNDEFINED;
		}

		context.component._setStartStop(StartStop.START);

		return State.RUNNING;
	}

}