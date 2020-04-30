package io.openems.edge.battery.soltaro.single.versionc.statemachine;

import io.openems.edge.battery.soltaro.single.versionc.enums.PreChargeControl;
import io.openems.edge.common.statemachine.StateHandler;

public class Running extends StateHandler<State, Context> {

	@Override
	public State getNextState(Context context) {
		if (context.component.hasFaults()) {
			return State.UNDEFINED;
		}

		if (context.component.getPreChargeControl() != PreChargeControl.RUNNING) {
			return State.UNDEFINED;
		}

		// TODO
		// this.checkAllowedCurrent();

		context.component._setReadyForWorking(true);

		return State.RUNNING;
	}

}
