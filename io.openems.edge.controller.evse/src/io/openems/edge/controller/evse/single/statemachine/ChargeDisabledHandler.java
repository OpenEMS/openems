package io.openems.edge.controller.evse.single.statemachine;

import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.evse.single.EvseSingleState;
import io.openems.edge.controller.evse.single.Mode;

public class ChargeDisabledHandler extends StateHandler<EvseSingleState, Context> {

	@Override
	public EvseSingleState runAndGetNextState(Context context) {
		if (!context.actions.abilities().isReadyForCharging() || !context.actions.abilities().isEvConnected()) {
			return EvseSingleState.UNDEFINED;
		}

		if (context.actions.applySetPoint() != null && context.actions.applySetPoint().value() != 0) {
			return EvseSingleState.CHARGE_RESUMING;
		}

		if (context.mode != Mode.ZERO) {
			return EvseSingleState.CHARGE_PAUSED;
		}

		context.applyAdjustedActions(a -> a.setCorrectApplySetPointByWatt(0).setPhaseSwitch(null));
		return EvseSingleState.CHARGE_DISABLED;
	}
}
