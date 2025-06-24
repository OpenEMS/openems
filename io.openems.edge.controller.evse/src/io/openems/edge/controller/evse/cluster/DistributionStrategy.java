package io.openems.edge.controller.evse.cluster;

enum DistributionStrategy {
	/**
	 * Distribute excess power equally among EVs.
	 */
	EQUAL_POWER,
	/**
	 * Distribute excess power in order of priority.
	 */
	BY_PRIORITY;
}