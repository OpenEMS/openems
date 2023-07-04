package io.openems.edge.controller.ess.cycle.statemachine;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.cycle.statemachine.StateMachine.State;
import io.openems.edge.ess.api.PowerConstraint;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

public class FinalSocHandler extends StateHandler<State, Context> {

	private final Logger log = LoggerFactory.getLogger(FinalSocHandler.class);

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		final var controller = context.getParent();
		final var ess = context.ess;
		final var config = context.config;

		if (ess.getSoc().get() == config.finalSoc()) {
			return State.FINISHED;
		}

		var power = context.getAcPower(ess, config.hybridEssMode(), config.power());
		if (ess.getSoc().get() > config.finalSoc()) {
			PowerConstraint.apply(ess, controller.id(), //
					Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, power);
			context.logInfo(this.log, "DISCHARGE with [" + power + " W]");
			return State.FINAL_SOC;
		}

		PowerConstraint.apply(ess, controller.id(), //
				Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, -power);
		context.logInfo(this.log, "CHARGE with [" + -power + " W]");

		return State.FINAL_SOC;
	}

	@Override
	protected void onExit(Context context) throws OpenemsNamedException {
		final var controller = context.getParent();
		controller.setLastStateChangeTime(LocalDateTime.now(context.componentManager.getClock()));
	}
}
