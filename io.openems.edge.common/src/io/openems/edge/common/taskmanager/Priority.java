package io.openems.edge.common.taskmanager;

public enum Priority {
	/**
	 * High-Priority.
	 * 
	 * <p>
	 * Execute Task on every Cycle.
	 */
	HIGH,
	/**
	 * Low-Priority.
	 * 
	 * <p>
	 * Execute Task 'once in a while'.
	 */
	LOW;
}
