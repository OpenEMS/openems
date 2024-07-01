package io.openems.edge.controller.ess.cycle.statemachine;

import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.cycle.statemachine.StateMachine.State;

public class CompletedCycleHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		final var controller = context.getParent();
		final var ess = context.ess;
		final var config = context.config;

		var completedCycles = controller.getCompletedCycles().orElse(0) + 1;
		controller._setCompletedCycles(completedCycles);

		if (completedCycles >= config.totalCycleNumber()) {
			return State.FINAL_SOC;
		}

		return switch (config.cycleOrder()) {
		case START_WITH_CHARGE -> State.START_CHARGE;
		case START_WITH_DISCHARGE -> State.START_DISCHARGE;
		case AUTO -> {
			int soc = ess.getSoc().get();
			if (soc < 50) {
				yield State.START_DISCHARGE;
			}
			yield State.START_CHARGE;
		}
		};
	}
}
