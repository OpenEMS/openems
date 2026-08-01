package io.openems.edge.bridge.modbus.api.task.hooks;

import io.openems.edge.bridge.modbus.api.AbstractModbusBridge;
import io.openems.edge.bridge.modbus.api.task.Task;

/**
 * Hook that is executed at various stages of a modbus task execution.
 * All methods are executed in the modbus bridge thread and are blocking.
 */
public abstract class TaskHook {
	/**
	 * Executed before the task is executed (e.g. before the modbus request is sent).
	 *
	 * @param bridge Current bridge
	 * @param task Current task
	 */
	public void preExecute(AbstractModbusBridge bridge, Task task) {
	}

	/**
	 * Executed after the modbus response is received and modbus bridge handled the response.
	 *
	 * @param bridge Current bridge
	 * @param task Current task
	 * @param state State of execution (NO_OP = No response available, ERROR = Response reading or execution failed, OK)
	 */
	public void execute(AbstractModbusBridge bridge, Task task, Task.ExecuteState state) {
	}

	/**
	 * Executed after the task was fully executed.
	 *
	 * @param bridge Current bridge
	 * @param task Current task
	 */
	public void postExecute(AbstractModbusBridge bridge, Task task) {
	}
}
