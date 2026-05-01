package io.openems.edge.energy.api.handler;

/**
 * Defines how the current period is handled when triggering a reschedule.
 */
public enum RescheduleMode {

	/**
	 * The current period will be optimized as part of the rescheduling process.
	 */
	OPTIMIZE_CURRENT_PERIOD(true),

	/**
	 * The current period will remain unchanged and will not be optimized.
	 */
	DO_NOT_UPDATE_CURRENT_PERIOD(false),

	;

	private final boolean optimizeCurrentPeriod;

	private RescheduleMode(boolean optimizeCurrentPeriod) {
		this.optimizeCurrentPeriod = optimizeCurrentPeriod;
	}

	/**
	 * Indicates whether the current period should be optimized.
	 *
	 * @return {@code true} if the current period should be optimized; {@code false}
	 *         if the current period should remain unchanged
	 */
	public boolean optimizeCurrentPeriod() {
		return this.optimizeCurrentPeriod;
	}
}