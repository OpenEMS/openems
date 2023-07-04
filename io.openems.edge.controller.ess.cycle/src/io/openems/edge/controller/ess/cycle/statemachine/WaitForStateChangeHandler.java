package io.openems.edge.controller.ess.cycle.statemachine;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.cycle.statemachine.StateMachine.State;

public class WaitForStateChangeHandler extends StateHandler<State, Context> {

	private final Logger log = LoggerFactory.getLogger(WaitForStateChangeHandler.class);

	@Override
	public State runAndGetNextState(Context context) {
		var controller = context.getParent();
		var nextState = controller.getNextState();
		var now = LocalDateTime.now(context.componentManager.getClock());
		var standbyTimeInMinutes = Duration.ofMinutes(context.config.standbyTime());

		if (now.minus(standbyTimeInMinutes.toSeconds(), ChronoUnit.SECONDS)
				.isAfter(controller.getLastStateChangeTime())) {
			return nextState;
		}

		context.logInfo(this.log, "Awaiting hysteresis for changing from ["//
				+ context.previousState + "] to [" + nextState + "]");

		return State.WAIT_FOR_STATE_CHANGE;
	}

	@Override
	protected void onExit(Context context) throws OpenemsNamedException {
		final var controller = context.getParent();
		controller.setLastStateChangeTime(LocalDateTime.now(context.componentManager.getClock()));
	}
}
