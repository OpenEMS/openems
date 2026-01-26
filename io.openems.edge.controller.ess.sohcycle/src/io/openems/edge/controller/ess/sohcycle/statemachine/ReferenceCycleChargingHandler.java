package io.openems.edge.controller.ess.sohcycle.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.sohcycle.EssSohCycleConstants;

public class ReferenceCycleChargingHandler extends StateHandler<StateMachine.State, Context> {
	private static final Logger log = LoggerFactory.getLogger(ReferenceCycleChargingHandler.class);

	@Override
	protected StateMachine.State runAndGetNextState(Context context) throws OpenemsError.OpenemsNamedException {
		final var result = context.applyChargingTarget(EssSohCycleConstants.MAX_SOC);

		if (result == null) {
			return StateMachine.State.REFERENCE_CYCLE_CHARGING;
		}

		if (!result.thresholdReached()) {
			context.logPowerState(log, StateMachine.State.REFERENCE_CYCLE_CHARGING, result,
					EssSohCycleConstants.MAX_SOC, true);
			return StateMachine.State.REFERENCE_CYCLE_CHARGING;
		}

		return StateMachine.State.REFERENCE_CYCLE_CHARGING_WAIT;
	}
}
