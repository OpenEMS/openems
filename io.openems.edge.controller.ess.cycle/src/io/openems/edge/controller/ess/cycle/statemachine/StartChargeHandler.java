package io.openems.edge.controller.ess.cycle.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.cycle.statemachine.StateMachine.State;

public class StartChargeHandler extends StateHandler<State, Context> {

	private final Logger log = LoggerFactory.getLogger(StartChargeHandler.class);

	@Override
	public State runAndGetNextState(Context context) throws IllegalArgumentException, OpenemsNamedException {
		if (context.config.maxSoc() == 100) {
			if (context.maxChargePower == 0) {
				// Wait for hysteresis
				if (context.waitForChangeState(State.START_CHARGE, State.CONTINUE_WITH_DISCHARGE)) {
					return State.CONTINUE_WITH_DISCHARGE;
				}
				return State.START_CHARGE;
			}
		} else if (context.ess.getSoc().orElse(0) >= context.config.maxSoc()) {
			// Wait for hysteresis
			if (context.waitForChangeState(State.START_CHARGE, State.CONTINUE_WITH_DISCHARGE)) {
				return State.CONTINUE_WITH_DISCHARGE;
			}
			return State.START_CHARGE;
		}

		// get max charge/discharge power
		var power = context.getChargePower();
		context.logInfo(this.log, "START CHARGE with [" + power + " W]" //
				+ " Current Cycle [ " + context.getParent().getCompletedCycles() + "] " //
				+ "out of " + context.config.totalCycleNumber() + "]");
		context.ess.setActivePowerLessOrEquals(power);

		return State.START_CHARGE;
	}
}
