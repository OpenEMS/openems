package io.openems.edge.controller.ess.cycle.statemachine;

import java.time.LocalDateTime;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
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

		if (completedCycles == config.totalCycleNumber()) {
			return State.FINAL_SOC;
		}

		if (!context.isEssSocDefined()) {
			return State.CONTINUE_WITH_DISCHARGE;
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

	@Override
	protected void onExit(Context context) throws OpenemsNamedException {
		final var controller = context.getParent();
		controller.setLastStateChangeTime(LocalDateTime.now(context.componentManager.getClock()));
	}
}
