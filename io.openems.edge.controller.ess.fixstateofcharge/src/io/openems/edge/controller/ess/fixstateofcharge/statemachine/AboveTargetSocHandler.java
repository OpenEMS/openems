package io.openems.edge.controller.ess.fixstateofcharge.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.fixstateofcharge.statemachine.StateMachine.State;

public class AboveTargetSocHandler extends StateHandler<State, Context> {

	@Override
	protected State runAndGetNextState(Context context) throws OpenemsNamedException {

		context.setRampPower(context.maxApparentPower * 0.05);
		context.setTargetPower(context.getLastTargetPower());

		var socState = Context.getSocState(context.soc, context.targetSoc);

		/*
		 * Check if SoC is no longer "AboveTargetSoc"
		 */
		if (!socState.equals(State.ABOVE_TARGET_SOC)) {
			return socState;
		}

		/*
		 * Maximum discharge if no target time specified or already passed
		 */
		if (!context.config.isTargetTimeSpecified() || context.passedTargetTime()) {
			context.setTargetPower(context.maxApparentPower);
			return State.ABOVE_TARGET_SOC;
		}

		/*
		 * Calculate target power.
		 */
		var targetPower = context.calculateTargetPower();
		targetPower = targetPower == null ? context.maxApparentPower : targetPower;

		context.setTargetPower(targetPower);
		return State.ABOVE_TARGET_SOC;
	}
}
