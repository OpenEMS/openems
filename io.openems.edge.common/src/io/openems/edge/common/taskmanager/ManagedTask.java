package io.openems.edge.common.taskmanager;

public interface ManagedTask {

	/**
	 * Gets the {@link Priority} of this {@link ManagedTask}.
	 * 
	 * @return the {@link Priority}
	 */
	public Priority getPriority();

	/**
	 * Get skip cycles.
	 *
	 * @return cycles the task should be delayed cycles
	 */
	public int getSkipCycles();
}
