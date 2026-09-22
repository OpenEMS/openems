package io.openems.edge.ess.stabl.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.ess.stabl.enums.ActivatePowerStage;

/**
 * Handles ERROR state.
 *
 * <p>
 * Attempts automatic recovery by issuing a software reset up to
 * {@link Context#MAX_RESET_ATTEMPTS} times. After each reset, waits
 * {@link Context#RESET_WAIT_TIME_MS} ms before retrying. If all attempts are
 * exhausted, logs an error requiring manual intervention.
 */
public class ErrorHandler {

	private final Logger log = LoggerFactory.getLogger(ErrorHandler.class);

	public ActivatePowerStage handle(Context context) {
		if (this.shouldAttemptReset(context)) {
			this.log.warn("System in ERROR; attempting reset #{}/{}", //
					context.resetAttemptCount + 1, Context.MAX_RESET_ATTEMPTS);
			context.getParent().setSoftwareReset(1);
			context.resetAttemptCount++;
			context.lastResetTime = System.currentTimeMillis();
			context.resetInProgress = true;
			return ActivatePowerStage.POWER_STAGE_OFF;
		}

		if (context.resetInProgress && this.hasResetWaitTimeElapsed(context)) {
			this.log.info("Reset wait completed, checking recovery");
			context.resetInProgress = false;
			return ActivatePowerStage.POWER_STAGE_OFF;
		}

		if (context.resetAttemptCount >= Context.MAX_RESET_ATTEMPTS) {
			this.log.error("ERROR recovery exhausted. Manual intervention required.");
		} else {
			long remaining = (Context.RESET_WAIT_TIME_MS - (System.currentTimeMillis() - context.lastResetTime)) / 1000;
			this.log.debug("Waiting for reset recovery… {} seconds remaining", remaining);
		}

		return ActivatePowerStage.POWER_STAGE_OFF;
	}

	/**
	 * Resets the error recovery state after the system has successfully recovered.
	 *
	 * @param context the shared context
	 */
	public void resetRecoveryState(Context context) {
		context.resetAttemptCount = 0;
		context.lastResetTime = 0;
		context.resetInProgress = false;
		this.log.info("System recovered from error, reset recovery state cleared");
	}

	private boolean shouldAttemptReset(Context context) {
		return context.resetAttemptCount < Context.MAX_RESET_ATTEMPTS //
				&& !context.resetInProgress //
				&& (context.lastResetTime == 0 || this.hasResetWaitTimeElapsed(context));
	}

	private boolean hasResetWaitTimeElapsed(Context context) {
		return (System.currentTimeMillis() - context.lastResetTime) >= Context.RESET_WAIT_TIME_MS;
	}

}
