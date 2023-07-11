package io.openems.edge.controller.ess.cycle.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.cycle.statemachine.StateMachine.State;
import io.openems.edge.ess.api.PowerConstraint;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

public class StartChargeHandler extends StateHandler<State, Context> {

	private final Logger log = LoggerFactory.getLogger(StartChargeHandler.class);

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		final var controller = context.getParent();
		final var ess = context.ess;
		final var config = context.config;

		if (config.maxSoc() == 100 && context.allowedChargePower == 0) {
			return context.waitForChangeState(State.START_CHARGE, State.CONTINUE_WITH_DISCHARGE);
		}

		if (ess.getSoc().get() >= config.maxSoc()) {
			return context.waitForChangeState(State.START_CHARGE, State.CONTINUE_WITH_DISCHARGE);
		}

		var power = context.getAcPower(ess, config.hybridEssMode(), config.power());
		PowerConstraint.apply(ess, controller.id(), //
				Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, -power);

		context.logInfo(this.log, "START CHARGE with [" + -power + " W]" //
				+ " Current Cycle [ " + controller.getCompletedCycles() + "] " //
				+ "out of " + config.totalCycleNumber() + "]");
		return State.START_CHARGE;
	}

	@Override
	protected void onExit(Context context) {
		context.updateLastStateChangeTime();
	}
}
