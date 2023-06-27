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
		// check abstractTask for plausibility
		checkTask(task);
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
	 * Checks a {@link Task} for plausibility.
	 *
	 * @param task the Task that should be checked
	 * @throws OpenemsException on error
	 */
	protected static synchronized void checkTask(Task task) throws OpenemsException {
		var address = task.getStartAddress();
		for (var element : task.getElements()) {
			if (element.getStartAddress() != address) {
				throw new OpenemsException("Start address is wrong. It is [" + element.getStartAddress() + "/0x"
						+ Integer.toHexString(element.getStartAddress()) + "] but should be [" + address + "/0x"
						+ Integer.toHexString(address) + "].");
			}
			address += element.getLength();
			// TODO: check BitElements
		}
	}

	/**
	 * Deactivate the {@link ModbusProtocol}.
	 */
	public void deactivate() {
		this.taskManager.getTasks() //
				.forEach(Task::deactivate);
	}
}
