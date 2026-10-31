package io.openems.edge.controller.ess.sohcycle.statemachine;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.sohcycle.EssSohCycleConstants;

/**
 * Recovery state entered from {@link StateMachine.State#PREPARE} when the ESS
 * currently does not allow discharging (e.g. after a previous deep discharge
 * where battery protection blocks further discharge although the reported SoC
 * is still above {@link EssSohCycleConstants#MIN_SOC}).
 *
 * <p>
 * Performs a controlled precharge (reusing the same charging-target calculation
 * as the other charging states) until discharging becomes available again, then
 * returns to {@link StateMachine.State#PREPARE} to continue the normal
 * discharge-to-0 flow.
 *
 * <p>
 * This is a single bounded recovery attempt. If discharge is not restored
 * within {@link EssSohCycleConstants#PREPARE_PRECHARGE_TIMEOUT_MINUTES}
 * minutes, the SoH cycle is aborted.
 */
public class PreparePrechargeHandler extends StateHandler<StateMachine.State, Context> {
	private static final Logger log = LoggerFactory.getLogger(PreparePrechargeHandler.class);

	private Instant enteredAt;

	@Override
	protected void onEntry(Context context) {
		this.enteredAt = context.getClock().instant();
	}

	@Override
	protected void onExit(Context context) throws OpenemsError.OpenemsNamedException {
		super.onExit(context);
		this.enteredAt = null;
	}

	@Override
	protected StateMachine.State runAndGetNextState(Context context) throws OpenemsError.OpenemsNamedException {
		if (context.isDischargeAvailable()) {
			context.requestZeroBatteryPower();
			context.logInfo(log, "PREPARE_PRECHARGE: AllowedDischargePower available again, returning to PREPARE");
			return StateMachine.State.PREPARE;
		}

		final var result = context.applyChargingTarget(EssSohCycleConstants.MAX_SOC);

		if (result == null) {
			return StateMachine.State.ERROR_ABORT;
		}

		if (this.isTimedOut(context)) {
			context.requestZeroBatteryPower();
			context.logInfo(log, String.format(
					"PREPARE_PRECHARGE: discharge remained unavailable for %d minutes; aborting SoH cycle",
					EssSohCycleConstants.PREPARE_PRECHARGE_TIMEOUT_MINUTES));
			return StateMachine.State.ERROR_ABORT;
		}

		context.logPowerState(log, StateMachine.State.PREPARE_PRECHARGE, result, EssSohCycleConstants.MAX_SOC, true);
		return StateMachine.State.PREPARE_PRECHARGE;
	}

	/**
	 * Checks whether total time in the recovery has reached
	 * {@link EssSohCycleConstants#PREPARE_PRECHARGE_TIMEOUT_MINUTES} minutes.
	 *
	 * <p>
	 * If {@link #enteredAt} was never set (e.g. {@code onEntry} was not called,
	 * as is the case for an initial State), timing starts lazily.
	 *
	 * @param context the state machine context
	 * @return {@code true} if the total timeout has elapsed
	 */
	private boolean isTimedOut(Context context) {
		if (this.enteredAt == null) {
			this.enteredAt = context.getClock().instant();
		}
		final Instant timeoutAt = this.enteredAt.plus(EssSohCycleConstants.PREPARE_PRECHARGE_TIMEOUT_MINUTES,
				ChronoUnit.MINUTES);
		return !timeoutAt.isAfter(context.getClock().instant());
	}
}
