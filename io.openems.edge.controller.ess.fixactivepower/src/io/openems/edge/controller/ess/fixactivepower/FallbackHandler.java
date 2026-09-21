package io.openems.edge.controller.ess.fixactivepower;

import java.time.Clock;

import io.openems.common.timedata.Timeout;
import io.openems.edge.controller.ess.fixactivepower.enums.Mode;
import io.openems.edge.ess.api.ManagedSymmetricEss;

public class FallbackHandler {

	protected static final int FALLBACK_TIMEOUT_MINUTES = 10;

	private final Timeout timeout = Timeout.ofMinutes(FALLBACK_TIMEOUT_MINUTES);
	private boolean isStarted;

	/**
	 * Checks if allowed charge/discharge power stayed at 0 W long enough to trigger
	 * fallback.
	 *
	 * @param ess   the ESS
	 * @param mode  the active single-shot mode
	 * @param clock the shared component clock
	 * @return true if fallback timeout is reached
	 */
	public boolean isFallbackTimeoutReached(ManagedSymmetricEss ess, Mode mode, Clock clock) {
		var allowedPower = switch (mode) {
		case CHARGE_ONCE -> ess.getAllowedChargePower();
		case DISCHARGE_ONCE -> ess.getAllowedDischargePower();
		default -> null;
		};

		// Ignore undefined values to avoid false positives during startup.
		if (allowedPower == null || !allowedPower.isDefined() || allowedPower.get() != 0) {
			this.clear();
			return false;
		}

		if (!this.isStarted) {
			this.timeout.start(clock);
			this.isStarted = true;
			return false;
		}

		return this.timeout.elapsed(clock);
	}

	/**
	 * Clears the currently running fallback timer.
	 */
	public void clear() {
		this.isStarted = false;
	}
}
