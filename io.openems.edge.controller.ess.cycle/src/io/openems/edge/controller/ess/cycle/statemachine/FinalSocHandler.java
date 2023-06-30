package io.openems.edge.controller.ess.cycle.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.cycle.statemachine.StateMachine.State;

public class FinalSocHandler extends StateHandler<State, Context> {

	private final Logger log = LoggerFactory.getLogger(FinalSocHandler.class);

	@Override
	public State runAndGetNextState(Context context) throws IllegalArgumentException, OpenemsNamedException {
		final var ess = context.ess;

		if (ess.getSoc().orElse(0) == context.config.finalSoc()) {
			return State.FINISHED;

		}
		if (ess.getSoc().orElse(0) > context.config.finalSoc()) {
			var power = Math.min(context.maxDischargePower, context.config.power());
			context.logInfo(this.log, "DISCHARGE with [" + power + " W]");
			context.ess.setActivePowerGreaterOrEquals(power);

		} else {
			var power = Math.max(context.maxChargePower, context.config.power() * -1);
			context.logInfo(this.log, "CHARGE with [" + power + " W]");
			context.ess.setActivePowerLessOrEquals(power);
		}
		return State.FINAL_SOC;
	}
}
