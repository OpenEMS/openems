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

public class StartDischargeHandler extends StateHandler<State, Context> {

	private final Logger log = LoggerFactory.getLogger(StartDischargeHandler.class);

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		final var controller = context.getParent();
		final var ess = context.ess;
		final var config = context.config;

		if (config.minSoc() == 0 && context.allowedDischargePower == 0) {
			return context.changeToNextState(State.CONTINUE_WITH_CHARGE);
		}

		if (ess.getSoc().get() <= config.minSoc()) {
			return context.changeToNextState(State.CONTINUE_WITH_CHARGE);
		}

		var power = context.getAcPower(ess, config.hybridEssMode(), config.power());
		PowerConstraint.apply(ess, controller.id(), //
				Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, power);

		context.logInfo(this.log, "START DISCHARGE with [" + power + " W]" //
				+ " Current Cycle [ " + controller.getCompletedCycles() + "] " //
				+ "out of " + config.totalCycleNumber() + "]");

		return State.START_DISCHARGE;
	}
}
