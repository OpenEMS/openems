package io.openems.edge.energy.api;

public enum RiskLevel {

	/**
	 * Less dependent on predictions. The storage system behavior is less likely to
	 * deviate from the predicted behavior.
	 */
	LOW(1.20),

	/**
	 * Moderately dependent on predictions. The storage system behavior may
	 * occasionally deviate from the predicted behavior but generally stays within
	 * expected parameters.
	 */
	MEDIUM(1.17),

	/**
	 * Heavily reliant on predictions. The storage system behavior is expected to
	 * closely align with the predicted behavior, but occasional over-consumption
	 * during peak pricing hours or under-consumption for self-sufficiency may still
	 * occur.
	 */
	HIGH(1.10);

	/** Used to incorporate charge/discharge efficiency. */
	public final double efficiencyFactor;

	private RiskLevel(double efficiencyFactor) {
		this.efficiencyFactor = efficiencyFactor;
	}
}
