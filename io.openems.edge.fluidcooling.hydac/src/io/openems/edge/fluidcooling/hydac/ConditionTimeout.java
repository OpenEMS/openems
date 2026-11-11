package io.openems.edge.fluidcooling.hydac;

import java.time.Clock;
import java.time.Duration;

import io.openems.common.timedata.Timeout;

public class ConditionTimeout {

	private final Timeout timeout;
	private boolean started;

	public ConditionTimeout(Duration duration) {
		this.timeout = Timeout.of(duration);
	}

	/**
	 * Updates the timeout based on the given condition.
	 *
	 * @param active whether the condition is currently active
	 * @param clock  the clock used for measuring the elapsed time
	 * @return {@code true} if the condition has been active for the configured
	 *         duration
	 */
	public boolean update(boolean active, Clock clock) {
		if (!active) {
			this.started = false;
			return false;
		}

		if (!this.started) {
			this.timeout.start(clock);
			this.started = true;
		}

		return this.timeout.elapsed(clock);
	}
}