package io.openems.edge.controller.evse.single.statemachine;

import static io.openems.edge.controller.evse.single.Types.History.allActivePowersAreZero;
import static io.openems.edge.controller.evse.single.Types.History.noSetPointsAreZero;

import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.evse.single.statemachine.StateMachine.State;

public class ChargingHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		// Apply Actions directly
		context.applyActions();

		final var history = context.history;
		if (history.isEntriesAreFullyInitialized() // History is fully populated and usable
				&& noSetPointsAreZero(history.streamAll()) // Non-Zero Set-Points had been sent
				&& allActivePowersAreZero(history.streamAll())) { // But all measured Active Powers were zero
			// -> Charging finished by EV
			return State.FINISHED_EV_STOP;
		}

		return State.CHARGING;
	}
}
