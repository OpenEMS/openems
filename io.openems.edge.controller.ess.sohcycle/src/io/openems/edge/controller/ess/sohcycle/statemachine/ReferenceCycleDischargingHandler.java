package io.openems.edge.controller.ess.sohcycle.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.sohcycle.EssSohCycleConstants;

public class ReferenceCycleDischargingHandler extends StateHandler<StateMachine.State, Context> {
	private static final Logger log = LoggerFactory.getLogger(ReferenceCycleDischargingHandler.class);

	@Override
	protected StateMachine.State runAndGetNextState(Context context) throws OpenemsError.OpenemsNamedException {
		final var result = context.applyDischargingTarget(EssSohCycleConstants.MIN_SOC);

		if (result == null) {
			return StateMachine.State.REFERENCE_CYCLE_DISCHARGING;
		}

		if (!result.thresholdReached()) {
			context.logPowerState(log, StateMachine.State.REFERENCE_CYCLE_DISCHARGING, result,
					EssSohCycleConstants.MIN_SOC, false);
			return StateMachine.State.REFERENCE_CYCLE_DISCHARGING;
		}

		return StateMachine.State.REFERENCE_CYCLE_DISCHARGING_WAIT;
	}
}
