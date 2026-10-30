package io.openems.edge.controller.evse.single.statemachine;

import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.evse.single.EvseSingleState;

public class UndefinedHandler extends StateHandler<EvseSingleState, Context> {

	@Override
	public EvseSingleState runAndGetNextState(Context context) {
		if (!context.actions.abilities().isEvConnected()) {
			return EvseSingleState.EV_NOT_CONNECTED;
		}

		if (!context.actions.abilities().isReadyForCharging()) {
			return EvseSingleState.NOT_READY;
		}

		return EvseSingleState.EV_CONNECTED;
	}
}
