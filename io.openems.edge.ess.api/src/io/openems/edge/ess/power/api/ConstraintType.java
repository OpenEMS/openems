package io.openems.edge.ess.power.api;

public enum ConstraintType {
	/**
	 * Static constraints. Those constraints stay forever.
	 */
	STATIC,
	/**
	 * Cycle constraints. Those constraints are cleared on every Cycle
	 */
	CYCLE;
}
