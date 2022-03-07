package io.openems.edge.controller.ess.cycle.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.cycle.statemachine.StateMachine.State;

public class StartDischargeHandler extends StateHandler<State, Context> {

	private final Logger log = LoggerFactory.getLogger(StartDischargeHandler.class);

	@Override
	public State runAndGetNextState(Context context) throws IllegalArgumentException, OpenemsNamedException {
		if (context.config.minSoc() < 1) {
			if (context.maxDischargePower == 0) {
				// Wait for hysteresis
				if (context.waitForChangeState(State.START_DISCHARGE, State.CONTINUE_WITH_CHARGE)) {
					return State.CONTINUE_WITH_CHARGE;
				}
				return State.START_DISCHARGE;
			}
		} else if (context.ess.getSoc().orElse(0) <= context.config.minSoc()) {
			// Wait for hysteresis
			if (context.waitForChangeState(State.START_DISCHARGE, State.CONTINUE_WITH_CHARGE)) {
				return State.CONTINUE_WITH_CHARGE;
			}
			return State.START_DISCHARGE;
		}

		var power = context.getDischargePower();
		context.logInfo(this.log, "START DISCHARGE with [" + power + " W]" //
				+ " Current Cycle [ " + context.getParent().getCompletedCycles() + "] " //
				+ "out of " + context.config.totalCycleNumber() + "]");
		context.ess.setActivePowerGreaterOrEquals(power);

		return State.START_DISCHARGE;
	}
}
