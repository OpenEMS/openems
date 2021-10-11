package io.openems.edge.controller.ess.cycle.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.cycle.statemachine.StateMachine.State;

public class FinishedHandler extends StateHandler<State, Context> {

	private final Logger log = LoggerFactory.getLogger(FinishedHandler.class);

	@Override
	public State runAndGetNextState(Context context) throws IllegalArgumentException, OpenemsNamedException {

		context.logInfo(this.log, "Current cycle [" + context.getParent().getCompletedCycles() + "] " //
				+ "completed out of [" + context.config.totalCycleNumber() + "] FINISHED");

		return State.FINISHED;
	}
}
