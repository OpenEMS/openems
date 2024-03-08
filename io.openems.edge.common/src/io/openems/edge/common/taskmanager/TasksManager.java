package io.openems.edge.common.taskmanager;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Manages a number of {@link ManagedTask}s with different priorities.
 *
 * <p>
 * A useful application for TasksManager is to provide a list of Tasks that need
 * to be handled on an OpenEMS Cycle run.
 *
 * @param <T> the type of the actual {@link ManagedTask}
 */
public class TasksManager<T extends ManagedTask> {

	private final List<T> tasks = new CopyOnWriteArrayList<>();

	private int nextTaskIndex = 0;

	@SafeVarargs
	public TasksManager(T... tasks) {
		this.addTasks(tasks);
	}

	/**
	 * Adds multiple Tasks.
	 *
	 * @param tasks an array of Tasks
	 */
	@SafeVarargs
	public final synchronized void addTasks(T... tasks) {
		for (T task : tasks) {
			this.addTask(task);
		}
	}

	/**
	 * Adds multiple Tasks.
	 *
	 * @param tasks an array of Tasks
	 */
	public void addTasks(List<T> tasks) {
		for (T task : tasks) {
			this.addTask(task);
		}
	}

	/**
	 * Adds a Task, taking its Priority in consideration.
	 *
	 * @param task the Task
	 */
	public synchronized void addTask(T task) {
		this.tasks.add(task);
	}

	/**
	 * Removes a Task.
	 *
	 * @param task the Task
	 */
	public synchronized void removeTask(T task) {
		this.tasks.remove(task);
	}

	/**
	 * Clears all Tasks lists.
	 */
	public synchronized void clearAll() {
		this.tasks.clear();
	}

	/**
	 * Gets the number of Tasks.
	 * 
	 * @return number of Tasks
	 */
	public synchronized int countTasks() {
		return this.tasks.size();
	}

	public synchronized int countTasks(long cycleIdx) {
		return (int)this.tasks.stream()
				.filter(t -> cycleIdx % (1 + t.getSkipCycles()) == 0)
				.count();
	}

	/**
	 * Gets all Tasks.
	 *
	 * @return a list of all Tasks.
	 */
	public synchronized List<T> getTasks() {
		return Collections.unmodifiableList(this.tasks);
	}

	/**
	 * Get all tasks with the given Priority.
	 *
	 * @param priority the Priority
	 * @return a list of Tasks
	 */
	public synchronized List<T> getTasks(Priority priority) {
		return this.tasks.stream() //
				.filter(t -> t.getPriority() == priority) //
				.collect(Collectors.toUnmodifiableList());
	}

	/**
	 * Gets tasks sequentially.
	 *
	 * @return the next task; null if there are no tasks
	 */
	public synchronized T getOneTask() {
		if (this.tasks.isEmpty()) {
			return null;
		}
		if (this.nextTaskIndex > this.tasks.size() - 1) {
			// start over
			this.nextTaskIndex = 0;
		}
		return this.tasks.get(this.nextTaskIndex++);
	}

}