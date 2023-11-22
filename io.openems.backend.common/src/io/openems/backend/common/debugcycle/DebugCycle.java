package io.openems.backend.common.debugcycle;

public interface DebugCycle {

	/**
	 * Cycle which gets executed frequently. Inside this method you could log or
	 * write debug metrics to a database.
	 */
	public void debugCycle();

}
