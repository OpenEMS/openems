package io.openems.edge.bridge.can.api;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import io.openems.edge.bridge.can.api.task.ReadTask;
import io.openems.edge.bridge.can.api.task.Task;
import io.openems.edge.bridge.can.api.task.WriteTask;
import io.openems.edge.common.taskmanager.TasksManager;

public class CanProtocol {

	/**
	 * The Parent component.
	 */
	private final AbstractOpenemsCanComponent parent;

	/**
	 * TaskManager for ReadTasks.
	 */
	private final TasksManager<ReadTask> readTaskManager = new TasksManager<>();

	/**
	 * TaskManager for WriteTasks.
	 */
	private final TasksManager<WriteTask> writeTaskManager = new TasksManager<>();

	private final Multimap<Integer, ReadTask> readTasksByCanAddress = Multimaps
			.synchronizedListMultimap(ArrayListMultimap.create());

	public CanProtocol(AbstractOpenemsCanComponent parent, Task... tasks) {
		this.parent = parent;
		for (Task task : tasks) {
			this.addTask(task);
		}
	}

	/**
	 * Adds Tasks to the Protocol.
	 *
	 * @param tasks the tasks
	 */
	public synchronized void addTasks(Task... tasks) {
		for (Task task : tasks) {
			this.addTask(task);
		}
	}

	/**
	 * Adds a Task to the Protocol.
	 *
	 * @param task the task
	 */
	public synchronized void addTask(Task task) {
		task.setParent(this.parent);
		if (task instanceof WriteTask) {
			this.writeTaskManager.addTask((WriteTask) task);
		}
		if (task instanceof ReadTask) {
			this.readTaskManager.addTask((ReadTask) task);
			this.readTasksByCanAddress.put(task.getCanAddress(), (ReadTask) task);
		}
	}

	public Multimap<Integer, ReadTask> getTasksByCanAddress() {
		return this.readTasksByCanAddress;
	}

	/**
	 * Removes a task from the protocol.
	 *
	 * @param task the task to be removed
	 */
	public synchronized void removeTask(Task task) {
		if (task instanceof ReadTask) {
			this.readTaskManager.removeTask((ReadTask) task);
			this.readTasksByCanAddress.removeAll(task.getCanAddress());
		}
		if (task instanceof WriteTask) {
			this.writeTaskManager.removeTask((WriteTask) task);
		}
	}

	/**
	 * Gets the Read-Tasks Manager.
	 *
	 * @return a the TaskManager
	 */
	public TasksManager<ReadTask> getReadTasksManager() {
		return this.readTaskManager;
	}

	/**
	 * Gets the Write-Tasks Manager.
	 *
	 * @return a the TaskManager
	 */
	public TasksManager<WriteTask> getWriteTasksManager() {
		return this.writeTaskManager;
	}

	/**
	 * Deactivates all tasks.
	 */
	public void deactivate() {
		var readTasks = this.readTaskManager.getTasks();
		for (ReadTask readTask : readTasks) {
			readTask.deactivate();
		}

		var writeTasks = this.writeTaskManager.getTasks();
		for (WriteTask writeTask : writeTasks) {
			writeTask.deactivate();
		}
	}

}
