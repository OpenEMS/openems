package io.openems.edge.controller.ess.stepsoc.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.stepsoc.statemachine.StateMachine.State;

public class UndefinedHandler extends StateHandler<State, Context> {

	private final Logger log = LoggerFactory.getLogger(UndefinedHandler.class);

	@Override
	public StateMachine.State runAndGetNextState(Context context) {
		final var soc = context.ess.getSoc();
		final var allowedCharge = context.ess.getAllowedChargePower();
		final var allowedDischarge = context.ess.getAllowedDischargePower();
		if (context.ess instanceof StartStoppable e && !e.isStarted()) {
			context.logInfo(this.log, "Ess " + context.ess.id() + " has not started yet!");
			return State.UNDEFINED;
		}

		if (context.power == 0) {
			context.logInfo(this.log, "Power is 0");
			return State.UNDEFINED;
		}

		if (!soc.isDefined()) {
			context.logInfo(this.log, "Ess SoC is not defined");
			return State.UNDEFINED;
		}

		if (!allowedCharge.isDefined()) {
			context.logInfo(this.log, "Ess allowedCharge is not defined");
			return State.UNDEFINED;
		}

		if (!allowedDischarge.isDefined()) {
			context.logInfo(this.log, "Ess allowedDischarge is not defined");
			return State.UNDEFINED;
		}

		return State.ALIGNING;
	}
}
