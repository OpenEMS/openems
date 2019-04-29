package io.openems.edge.common.taskmanager;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Manages a number of {@link TasksManager}s.
 * 
 * <p>
 * A useful application for MetaTasksManager is to provide a list of Tasks that
 * need to be handled on an OpenEMS Cycle run.
 * 
 * @param <T>
 */
public class MetaTasksManager<T extends ManagedTask> {

	private final List<TasksManager<T>> tasksManagers = new ArrayList<>();
	private Map<Priority, Queue<T>> nextTasks;

	public MetaTasksManager() {
		// initialize Queues for next tasks
		EnumMap<Priority, Queue<T>> nextTasks = new EnumMap<>(Priority.class);
		for (Priority priority : Priority.values()) {
			nextTasks.put(priority, new LinkedList<>());
		}
		this.nextTasks = nextTasks;
	}

	/**
	 * Adds multiple TasksManagers.
	 * 
	 * @param tasks an array of TasksManagers
	 */
	@SafeVarargs
	public final synchronized void addTasksManagers(TasksManager<T>... tasksManagers) {
		for (TasksManager<T> tasksManager : tasksManagers) {
			this.addTasksManager(tasksManager);
		}
	}

	/**
	 * Adds multiple TasksManagers.
	 * 
	 * @param tasks a list of TasksManagers
	 */
	public void addTasksManagers(List<TasksManager<T>> tasksManagers) {
		for (TasksManager<T> tasksManager : tasksManagers) {
			this.addTasksManager(tasksManager);
		}
	}

	/**
	 * Adds a TasksManager.
	 * 
	 * @param task the TasksManager
	 */
	public synchronized void addTasksManager(TasksManager<T> tasksManager) {
		this.tasksManagers.add(tasksManager);
	}

	/**
	 * Removes a TasksManager.
	 * 
	 * @param task the TasksManager
	 */
	public synchronized void removeTasksManager(TasksManager<T> tasksManager) {
		this.tasksManagers.remove(tasksManager);
	}

	/**
	 * Gets one task that with the given Priority sequentially.
	 * 
	 * @return the next task; null if there are no tasks with the given Priority
	 */
	public synchronized T getOneTask(Priority priority) {
		Queue<T> tasks = this.nextTasks.get(priority);
		if (tasks.isEmpty()) {
			// refill the queue
			for (TasksManager<T> tasksManager : this.tasksManagers) {
				tasks.addAll(tasksManager.getAllTasks(priority));
			}
		}

		// returns the head or 'null' if the queue is still empty after refilling it
		return tasks.poll();
	}

	/**
	 * Gets all Tasks with the given Priority.
	 * 
	 * @param priority the priority
	 * @return a list of tasks
	 */
	public List<T> getAllTasks(Priority priority) {
		List<T> tasks = new ArrayList<>();
		for (TasksManager<T> tasksManager : this.tasksManagers) {
			tasks.addAll(tasksManager.getAllTasks(priority));
		}
		return tasks;
	}

}