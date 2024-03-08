package io.openems.edge.common.taskmanager;

public interface ManagedTask {

	/**
	 * Gets the {@link Priority} of this {@link ManagedTask}.
	 * 
	 * @return the {@link Priority}
	 */
	public Priority getPriority();

	public int getSkipCycles();
}
