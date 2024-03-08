package io.openems.edge.bridge.modbus.api;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.task.Task;
import io.openems.edge.common.taskmanager.TasksManager;

public class ModbusProtocol {

	/**
	 * The Parent component.
	 */
	private final AbstractOpenemsModbusComponent parent;

	/**
	 * TaskManager for ReadTasks.
	 */
	private final TasksManager<Task> taskManager = new TasksManager<>();

	/**
	 * Creates a new {@link ModbusProtocol}.
	 *
	 * @param parent the {@link AbstractOpenemsModbusComponent} parent
	 * @param tasks  the {@link Task}s
	 * @throws OpenemsException on error
	 */
	public ModbusProtocol(AbstractOpenemsModbusComponent parent, Task... tasks) throws OpenemsException {
		this.parent = parent;
		this.addTasks(tasks);
	}

	/**
	 * Adds Tasks to the Protocol.
	 *
	 * @param tasks the tasks
	 * @throws OpenemsException on error
	 */
	public synchronized void addTasks(Task... tasks) throws OpenemsException {
		for (Task task : tasks) {
			this.addTask(task);
		}
	}

	/**
	 * Adds a Task to the Protocol.
	 *
	 * @param task the task
	 * @throws OpenemsException on plausibility error
	 */
	public synchronized void addTask(Task task) throws OpenemsException {
		// add the the parent to the Task
		task.setParent(this.parent);
		// fill taskManager
		this.taskManager.addTask(task);
	}

	/**
	 * Removes a Task from the Protocol.
	 *
	 * @param task the task
	 */
	public synchronized void removeTask(Task task) {
		this.taskManager.removeTask(task);
	}

	/**
	 * Gets the Read-Tasks Manager.
	 *
	 * @return a the TaskManager
	 */
	public TasksManager<Task> getTaskManager() {
		return this.taskManager;
	}

	/**
	 * Deactivate the {@link ModbusProtocol}.
	 */
	public void deactivate() {
		this.taskManager.getTasks() //
				.forEach(Task::deactivate);
	}

	public ModbusProtocol setSkipCycles(int cycles) {
		this.taskManager.getTasks() //
				.forEach(t -> t.setSkipCycles(cycles));
		return this;
	}
}
