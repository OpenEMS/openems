package io.openems.edge.controller.evse.single.statemachine;

import static io.openems.edge.controller.evse.single.Types.History.allActivePowersAreZero;

import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.evse.single.statemachine.StateMachine.State;

public class FinishedEvStopHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		// Allow charge with minimum power
		context.applyMinSetPointActions();

		final var history = context.history;
		if (!allActivePowersAreZero(history.streamAll())) { // Non-Zero Active Powers were measured
			// -> EV is again charging
			return State.CHARGING;
		}

		return State.FINISHED_EV_STOP;
	}
}
