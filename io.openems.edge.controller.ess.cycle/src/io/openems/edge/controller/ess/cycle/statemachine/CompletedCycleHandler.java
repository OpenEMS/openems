package io.openems.edge.controller.ess.cycle.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.cycle.statemachine.StateMachine.State;

public class CompletedCycleHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) throws IllegalArgumentException, OpenemsNamedException {
		var completedCycles = context.getParent().getCompletedCycles().orElse(0) + 1;
		context.getParent()._setCompletedCycles(completedCycles);

		if (completedCycles == context.config.totalCycleNumber()) {
			return State.FINAL_SOC;
		}

		switch (context.previousState) {
		case CONTINUE_WITH_CHARGE:
			return State.START_DISCHARGE;
		case CONTINUE_WITH_DISCHARGE:
			return State.START_CHARGE;

		case COMPLETED_CYCLE:
		case FINAL_SOC:
		case FINISHED:
		case START_CHARGE:
		case START_DISCHARGE:
		case UNDEFINED:
			break;
		}
		return State.COMPLETED_CYCLE;
	}
}
