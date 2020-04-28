package io.openems.edge.battery.soltaro.cluster.versionc.statemachine;

import io.openems.edge.battery.soltaro.cluster.enums.ContactorControl;
import io.openems.edge.battery.soltaro.cluster.versionc.statemachine.StateMachine.Context;

public class Running extends State.Handler {

	@Override
	public State getNextState(Context context) {
		if (context.component.hasFaults()) {
			return State.UNDEFINED;
		}

		ContactorControl commonContactorControlState = context.component.getCommonContactorControlState()
				.orElse(ContactorControl.UNDEFINED);
		if (commonContactorControlState != ContactorControl.ON_GRID) {
			return State.UNDEFINED;
		}

		context.component.setReadyForWorking(true);

		return State.RUNNING;
	}

}
