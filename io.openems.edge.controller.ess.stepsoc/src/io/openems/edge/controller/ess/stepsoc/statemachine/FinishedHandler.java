package io.openems.edge.controller.ess.stepsoc.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.stepsoc.statemachine.StateMachine.State;

public class FinishedHandler extends StateHandler<State, Context> {

	private final Logger log = LoggerFactory.getLogger(FinishedHandler.class);

	@Override
	public StateMachine.State runAndGetNextState(Context context) {
		context.logInfo(this.log, "Step SoC finished. Target SoC reached, no further action required");
		return State.FINISHED;
	}
}
