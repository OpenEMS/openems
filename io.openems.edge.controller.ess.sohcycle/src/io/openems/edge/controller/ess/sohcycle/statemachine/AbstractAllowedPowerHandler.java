package io.openems.edge.controller.ess.sohcycle.statemachine;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;

/**
 * Tracks continuous explicit AllowedPower blocking within a single state.
 */
public abstract class AbstractAllowedPowerHandler extends StateHandler<StateMachine.State, Context> {

	private static final Duration BLOCKED_DURATION = Duration.ofMinutes(5);

	private Instant blockedSince;

	/**
	 * Checks whether the required capability has been explicitly blocked for five
	 * minutes. Available or undefined capability must be passed as {@code false},
	 * clearing the timer. Starts lazily without depending on {@code onEntry()}.
	 *
	 * @param clock             the controller clock
	 * @param explicitlyBlocked the charge or discharge blocked predicate
	 * @return whether continuous blocking has reached five minutes
	 */
	protected final boolean isBlockedForFiveMinutes(Clock clock, boolean explicitlyBlocked) {
		if (!explicitlyBlocked) {
			this.blockedSince = null;
			return false;
		}

		final var now = clock.instant();
		if (this.blockedSince == null) {
			this.blockedSince = now;
		}
		return !now.isBefore(this.blockedSince.plus(BLOCKED_DURATION));
	}

	/**
	 * Clears state-local timing. Overrides must call {@code super.onExit(context)}
	 * before performing their own cleanup so the timer is cleared even if cleanup
	 * fails.
	 */
	@Override
	protected void onExit(Context context) throws OpenemsNamedException {
		this.blockedSince = null;
		super.onExit(context);
	}
}
