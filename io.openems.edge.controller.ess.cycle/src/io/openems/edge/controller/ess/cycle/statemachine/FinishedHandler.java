package io.openems.edge.controller.ess.cycle.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.cycle.statemachine.StateMachine.State;

public class FinishedHandler extends StateHandler<State, Context> {

	private final Logger log = LoggerFactory.getLogger(FinishedHandler.class);

	@Override
	public State runAndGetNextState(Context context) {
		final var controller = context.getParent();
		final var config = context.config;

		context.logInfo(this.log, "Current cycle [" + controller.getCompletedCycles() + "] " //
				+ "completed out of [" + config.totalCycleNumber() + "] FINISHED");

		return State.FINISHED;
	}
}
