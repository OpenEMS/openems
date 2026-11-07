package io.openems.edge.controller.evse.single.statemachine;

import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.evse.single.EvseSingleState;

public class EvNotConnectedHandler extends StateHandler<EvseSingleState, Context> {

	@Override
	public EvseSingleState runAndGetNextState(Context context) {
		// Allow charge with minimum power
		context.applyMinSetPointActions();

		if (context.actions.abilities().isEvConnected()) {
			return EvseSingleState.UNDEFINED;
		}

		if (context.actions.phaseSwitch() != null) {
			return switch (context.actions.phaseSwitch().direction()) {
			case TO_SINGLE_PHASE -> EvseSingleState.PHASE_SWITCH_TO_SINGLE_PHASE;
			case TO_THREE_PHASE -> EvseSingleState.PHASE_SWITCH_TO_THREE_PHASE;
			};
		}

		return EvseSingleState.EV_NOT_CONNECTED;
	}
}
