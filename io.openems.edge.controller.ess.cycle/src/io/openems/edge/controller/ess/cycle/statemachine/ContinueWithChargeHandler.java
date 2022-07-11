package io.openems.edge.controller.ess.cycle.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.cycle.statemachine.StateMachine.State;

public class ContinueWithChargeHandler extends StateHandler<State, Context> {

	private final Logger log = LoggerFactory.getLogger(ContinueWithChargeHandler.class);

	@Override
	public State runAndGetNextState(Context context) throws IllegalArgumentException, OpenemsNamedException {
		final var ess = context.ess;

		if (context.config.maxSoc() > 99) {
			if (context.maxChargePower == 0) {
				// Wait for hysteresis
				if (context.waitForChangeState(State.CONTINUE_WITH_CHARGE, State.COMPLETED_CYCLE)) {
					return State.COMPLETED_CYCLE;
				}
				return State.CONTINUE_WITH_CHARGE;
			}
		} else if (ess.getSoc().orElse(0) >= context.config.maxSoc()) {
			// Wait for hysteresis
			if (context.waitForChangeState(State.CONTINUE_WITH_CHARGE, State.COMPLETED_CYCLE)) {
				return State.COMPLETED_CYCLE;
			}
			return State.CONTINUE_WITH_CHARGE;
		}

		// get max charge/discharge power
		var power = context.getChargePower();
		context.logInfo(this.log, "CONTINUE CHARGE with [" + power + " W]" //
				+ " Current Cycle [ " + context.getParent().getCompletedCycles() + "] " //
				+ "out of " + context.config.totalCycleNumber() + "]");
		context.ess.setActivePowerLessOrEquals(power);

		return State.CONTINUE_WITH_CHARGE;
	}
}
