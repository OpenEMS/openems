package io.openems.edge.controller.ess.sohcycle.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.controller.ess.sohcycle.EssSohCycleConstants;

public class PrepareHandler extends AbstractAllowedPowerHandler {
	private static final Logger log = LoggerFactory.getLogger(PrepareHandler.class);

	@Override
	protected StateMachine.State runAndGetNextState(Context context) throws OpenemsError.OpenemsNamedException {
		final var result = context.applyDischargingTarget(EssSohCycleConstants.MIN_SOC);

		if (result == null) {
			return StateMachine.State.ERROR_ABORT;
		}

		if (result.thresholdReached()) {
			return this.getSuccessfulNextState(context);
		}

		final boolean dischargeBlocked = context.isDischargeBlocked();
		if (dischargeBlocked && !context.isPreparePrechargeUsed()) {
			context.markPreparePrechargeUsed();
			context.requestZeroBatteryPower();
			context.logInfo(log, "PREPARE: AllowedDischargePower is 0, entering PREPARE_PRECHARGE");
			return StateMachine.State.PREPARE_PRECHARGE;
		}

		if (this.isBlockedForFiveMinutes(context.getClock(), dischargeBlocked)) {
			context.requestZeroBatteryPower();
			context.logInfo(log, "PREPARE: discharge remained blocked for 5 minutes; accepting lower endpoint");
			return this.getSuccessfulNextState(context);
		}

		context.logPowerState(log, StateMachine.State.PREPARE, result, EssSohCycleConstants.MIN_SOC, false);
		return StateMachine.State.PREPARE;
	}

	private StateMachine.State getSuccessfulNextState(Context context) {
		return context.getConfig().referenceCycleEnabled() ? StateMachine.State.REFERENCE_CYCLE_CHARGING
				: StateMachine.State.MEASUREMENT_CYCLE_CHARGING;
	}
}
