package io.openems.edge.controller.ess.cycle.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.cycle.statemachine.StateMachine.State;

public class ContinueWithDischargeHandler extends StateHandler<State, Context> {

	private final Logger log = LoggerFactory.getLogger(ContinueWithDischargeHandler.class);

	@Override
	public State runAndGetNextState(Context context) throws IllegalArgumentException, OpenemsNamedException {
		if (context.config.minSoc() == 0) {
			if (context.maxDischargePower == 0) {
				// Wait for hysteresis
				if (context.waitForChangeState(State.CONTINUE_WITH_DISCHARGE, State.COMPLETED_CYCLE)) {
					return State.COMPLETED_CYCLE;
				}
				return State.CONTINUE_WITH_DISCHARGE;
			}
		} else if (context.ess.getSoc().orElse(0) <= context.config.minSoc()) {
			// Wait for hysteresis
			if (context.waitForChangeState(State.CONTINUE_WITH_DISCHARGE, State.COMPLETED_CYCLE)) {
				return State.COMPLETED_CYCLE;
			}
			return State.CONTINUE_WITH_DISCHARGE;
		}

		var power = context.getDischargePower();
		context.logInfo(this.log, "CONTINUE DISCHARGE with [" + power + " W]" //
				+ " Current Cycle [ " + context.getParent().getCompletedCycles() + "] " //
				+ "out of " + context.config.totalCycleNumber() + "]");
		context.ess.setActivePowerGreaterOrEquals(power);

		return State.CONTINUE_WITH_DISCHARGE;
	}
}
