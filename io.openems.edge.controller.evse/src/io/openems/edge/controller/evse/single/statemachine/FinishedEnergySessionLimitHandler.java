package io.openems.edge.controller.evse.single.statemachine;

import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.evse.single.EvseSingleState;

public class FinishedEnergySessionLimitHandler extends StateHandler<EvseSingleState, Context> {

	@Override
	public EvseSingleState runAndGetNextState(Context context) {
		// Stop charging
		context.applyAdjustedActions(b -> b //
				.setApplyZeroSetPoint() //
				.setPhaseSwitch(null));

		return EvseSingleState.FINISHED_ENERGY_SESSION_LIMIT;
	}
}
