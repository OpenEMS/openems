package io.openems.edge.controller.evse.single.statemachine;

import static io.openems.edge.controller.evse.single.Types.History.allActivePowersAreZero;

import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.evse.single.EvseSingleState;

public class FinishedEvStopHandler extends StateHandler<EvseSingleState, Context> {

	@Override
	public EvseSingleState runAndGetNextState(Context context) {
		// Allow charge with minimum power
		context.applyMinSetPointActions();

		final var history = context.history;
		if (!allActivePowersAreZero(history.streamAll())) { // Non-Zero Active Powers were measured
			// -> EV is again charging
			return EvseSingleState.CHARGING;
		}

		return EvseSingleState.FINISHED_EV_STOP;
	}
}
