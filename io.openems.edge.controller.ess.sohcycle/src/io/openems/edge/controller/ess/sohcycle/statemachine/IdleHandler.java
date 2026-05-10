package io.openems.edge.controller.ess.sohcycle.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.sohcycle.statemachine.StateMachine.State;

public class IdleHandler extends StateHandler<State, Context> {
	private static final Logger log = LoggerFactory.getLogger(IdleHandler.class);

	@Override
	public State runAndGetNextState(Context context) {
		if (context.config.isRunning()) {
			context.logInfo(log,"Starting SoH Cycle");
			return State.PREPARE;
		}
		return StateMachine.State.IDLE;
	}

	@Override
	protected void onExit(Context context) throws OpenemsError.OpenemsNamedException {
		super.onExit(context);
		context.resetController();
	}
}
