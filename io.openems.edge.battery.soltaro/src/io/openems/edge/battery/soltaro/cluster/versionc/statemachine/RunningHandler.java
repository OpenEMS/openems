package io.openems.edge.battery.soltaro.cluster.versionc.statemachine;

import io.openems.edge.battery.soltaro.cluster.versionc.ClusterVersionC;
import io.openems.edge.battery.soltaro.cluster.versionc.statemachine.StateMachine.State;
import io.openems.edge.battery.soltaro.single.versionc.enums.PreChargeControl;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;

public class RunningHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		ClusterVersionC battery = context.getParent();

		if (battery.hasFaults()) {
			return State.UNDEFINED;
		}

		PreChargeControl commonPreChargeControl = battery.getCommonPreChargeControl()
				.orElse(PreChargeControl.UNDEFINED);
		if (commonPreChargeControl != PreChargeControl.RUNNING) {
			return State.UNDEFINED;
		}

		battery._setStartStop(StartStop.START);

		return State.RUNNING;
	}

}