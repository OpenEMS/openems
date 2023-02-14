package io.openems.edge.controller.ess.fixstateofcharge.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.fixstateofcharge.statemachine.StateMachine.State;

public class BelowTargetSocHandler extends StateHandler<State, Context> {

	@Override
	protected State runAndGetNextState(Context context) throws OpenemsNamedException {

		context.setRampPower(context.maxApparentPower * 0.05);
		context.setTargetPower(context.getLastTargetPower());

		var socState = Context.getSocState(context.soc, context.targetSoc);

		/*
		 * Check if SoC is no longer "BelowTargetSoc"
		 */
		if (!socState.equals(State.BELOW_TARGET_SOC)) {
			return socState;
		}

		/*
		 * Maximum charge if no target time specified or already passed
		 */
		if (!context.config.isTargetTimeSpecified() || context.passedTargetTime()) {

			context.setTargetPower(context.maxApparentPower * -1);
			return State.BELOW_TARGET_SOC;
		}

		/*
		 * Calculate target power.
		 */
		var targetPower = context.calculateTargetPower();
		targetPower = targetPower == null ? context.maxApparentPower : targetPower;

		context.setTargetPower(targetPower * -1);
		return State.BELOW_TARGET_SOC;
	}
}
