package io.openems.edge.controller.evse.single.statemachine;

import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.evse.single.EvseSingleState;
import io.openems.edge.controller.evse.single.Mode;

public class FinishedEnergySessionLimitHandler extends StateHandler<EvseSingleState, Context> {

	@Override
	public EvseSingleState runAndGetNextState(Context context) {
		if (!context.actions.abilities().isReadyForCharging() || !context.actions.abilities().isEvConnected()) {
			return EvseSingleState.UNDEFINED;
		}
		if (context.mode == Mode.ZERO) {
			return EvseSingleState.CHARGE_DISABLED;
		}
		if (!context.reachedSessionLimit) {
			return EvseSingleState.CHARGE_RESUMING;
		}

		// Stop charging
		context.applyAdjustedActions(b -> b //
				.setApplyZeroSetPoint() //
				.setPhaseSwitch(null));

		return EvseSingleState.FINISHED_ENERGY_SESSION_LIMIT;
	}
}
