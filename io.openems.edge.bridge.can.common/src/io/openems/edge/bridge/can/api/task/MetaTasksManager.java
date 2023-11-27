package io.openems.edge.bridge.can.api.task;

import java.util.EnumMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import io.openems.edge.common.taskmanager.ManagedTask;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.taskmanager.TasksManager;

/**
 * Manages a number of {@link TasksManager}s.
 *
 * <p>
 * A useful application for MetaTasksManager is to provide a list of Tasks that
 * need to be handled on an OpenEMS Cycle run.
 *
 * @param <T> the type of the actual {@link ManagedTask}
 */
public class MetaTasksManager<T extends ManagedTask> {

	private final Multimap<String, TasksManager<T>> tasksManagers = Multimaps
			.synchronizedListMultimap(ArrayListMultimap.create());
	private final Map<Priority, Queue<T>> nextTasks;

	public MetaTasksManager() {
		// initialize Queues for next tasks
		var nextTasks = new EnumMap<Priority, Queue<T>>(Priority.class);
		for (Priority priority : Priority.values()) {
			nextTasks.put(priority, new LinkedList<>());
		}
		this.nextTasks = nextTasks;
	}

	/**
	 * Adds a TasksManager.
	 *
	 * @param sourceId     a source identifier
	 * @param tasksManager the TasksManager
	 */
	public synchronized void addTasksManager(String sourceId, TasksManager<T> tasksManager) {
		this.tasksManagers.put(sourceId, tasksManager);
	}

	/**
	 * Removes a TasksManager.
	 *
	 * @param sourceId     a source identifier
	 * @param tasksManager the TasksManager
	 */
	public synchronized void removeTasksManager(String sourceId, TasksManager<T> tasksManager) {
		this.tasksManagers.remove(sourceId, tasksManager);
	}

	/**
	 * Removes all TasksManagers with the given Source-ID.
	 *
	 * @param sourceId a source identifier
	 */
	public synchronized void removeTasksManager(String sourceId) {
		this.tasksManagers.removeAll(sourceId);
	}

	/**
	 * Gets the next task with the given Priority sequentially.
	 *
	 * @param priority the {@link Priority}
	 * @return the next task; null if there are no tasks with the given Priority
	 */
	public synchronized T getOneTask(Priority priority) {
		var tasks = this.nextTasks.get(priority);
		if (tasks.isEmpty()) {
			// refill the queue
			for (TasksManager<T> tasksManager : this.tasksManagers.values()) {
				tasks.addAll(tasksManager.getTasks(priority));
			}
		}

		// returns the head or 'null' if the queue is still empty after refilling it
		return tasks.poll();
	}

	/**
	 * Gets all Tasks with the given Priority by their Source-ID.
	 *
	 * @param priority the priority
	 * @return a list of tasks
	 */
	public Multimap<String, T> getAllTasksBySourceId(Priority priority) {
		Multimap<String, T> result = ArrayListMultimap.create();
		for (Entry<String, TasksManager<T>> entry : this.tasksManagers.entries()) {
			result.putAll(entry.getKey(), entry.getValue().getTasks(priority));
		}
		return result;
	}

	/**
	 * Gets all Tasks with by their Source-ID.
	 *
	 * @return a list of tasks
	 */
	public Multimap<String, T> getAllTasksBySourceId() {
		Multimap<String, T> result = ArrayListMultimap.create();
		for (Entry<String, TasksManager<T>> entry : this.tasksManagers.entries()) {
			result.putAll(entry.getKey(), entry.getValue().getTasks());
		}
		return result;
	}

	// oems Start
	/**
	 * Gets all Tasks with by their Source-ID.
	 * 
	 * @param id the source Id.
	 *
	 * @return a list of tasks
	 */
	public Multimap<String, T> getAllTasksBySourceId(String id) {
		Multimap<String, T> result = ArrayListMultimap.create();
		this.tasksManagers.get(id).forEach(entry -> {
			result.putAll(id, entry.getTasks());
		});
		return result;
	}

	// oems End

	/**
	 * Does this {@link TasksManager} have any Tasks?.
	 *
	 * @return true if there are Tasks
	 */
	public boolean hasTasks() {
		return !this.tasksManagers.isEmpty();
	}

}