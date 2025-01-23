package io.openems.edge.bridge.modbus.api.worker.internal;

public interface TasksSupplier {

	/**
	 * Supplies the Tasks for one Cycle.
	 * 
	 * @param defectiveComponents the {@link DefectiveComponents} handler
	 * @return a {@link CycleTasks} object
	 */
	public CycleTasks getCycleTasks(DefectiveComponents defectiveComponents);

	/**
	 * Gets the total number of tasks.
	 * 
	 * @return total number of tasks
	 */
	public int getTotalNumberOfTasks();

}
