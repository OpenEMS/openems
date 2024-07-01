package io.openems.edge.controller.ess.timeofusetariff;

public enum RiskLevel {

	/**
	 * Less dependent on predictions. The storage system behavior is less likely to
	 * deviate from the predicted behavior.
	 */
	LOW,

	/**
	 * Moderately dependent on predictions. The storage system behavior may
	 * occasionally deviate from the predicted behavior but generally stays within
	 * expected parameters.
	 */
	MEDIUM,

	/**
	 * Heavily reliant on predictions. The storage system behavior is expected to
	 * closely align with the predicted behavior, but occasional over-consumption
	 * during peak pricing hours or under-consumption for self-sufficiency may still
	 * occur.
	 */
	HIGH

}
