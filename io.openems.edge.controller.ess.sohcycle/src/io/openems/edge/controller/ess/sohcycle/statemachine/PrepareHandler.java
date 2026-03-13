package io.openems.edge.controller.ess.sohcycle.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.sohcycle.EssSohCycleConstants;

public class PrepareHandler extends StateHandler<StateMachine.State, Context> {
	private static final Logger log = LoggerFactory.getLogger(PrepareHandler.class);

	@Override
	protected StateMachine.State runAndGetNextState(Context context) throws OpenemsError.OpenemsNamedException {
		final var result = context.applyDischargingTarget(EssSohCycleConstants.MIN_SOC);

		if (result == null) {
			return StateMachine.State.ERROR_ABORT;
		}

		if (!result.thresholdReached()) {
			context.logPowerState(log, StateMachine.State.PREPARE, result,
					EssSohCycleConstants.MIN_SOC, false);
			return StateMachine.State.PREPARE;
		}

		if (context.getConfig().referenceCycleEnabled()) {
			return StateMachine.State.REFERENCE_CYCLE_CHARGING;
		}

		return StateMachine.State.MEASUREMENT_CYCLE_CHARGING;

	}
}
