package io.openems.edge.battery.soltaro.cluster.versionc.statemachine;

import io.openems.edge.battery.soltaro.cluster.versionc.statemachine.StateMachine.Context;
import io.openems.edge.battery.soltaro.single.versionc.enums.PreChargeControl;

public class Running extends State.Handler {

	@Override
	public State getNextState(Context context) {
		if (context.component.hasFaults()) {
			return State.UNDEFINED;
		}

		PreChargeControl commonPreChargeControl = context.component.getCommonPreChargeControl()
				.orElse(PreChargeControl.UNDEFINED);
		if (commonPreChargeControl != PreChargeControl.RUNNING) {
			return State.UNDEFINED;
		}

		context.component.setReadyForWorking(true);

		return State.RUNNING;
	}

}
