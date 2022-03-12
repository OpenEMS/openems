package io.openems.edge.common.taskmanager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages a number of {@link ManagedTask}s with different priorities.
 *
 * <p>
 * A useful application for TasksManager is to provide a list of Tasks that need
 * to be handled on an OpenEMS Cycle run.
 *
 * @param <T>
 */
public class TasksManager<T extends ManagedTask> {

	private final List<T> allTasks = new CopyOnWriteArrayList<>();

	private final List<T> prioHighTasks = new CopyOnWriteArrayList<>();
	private final List<T> prioLowTasks = new CopyOnWriteArrayList<>();
	private final List<T> prioOnceTasks = new CopyOnWriteArrayList<>();

	private final Queue<T> nextLowTasks = new LinkedList<>();
	private final Queue<T> nextOnceTasks = new LinkedList<>();

	private int nextTaskIndex = 0;
	private final EnumMap<Priority, Integer> nextTaskIndexPerPriority = new EnumMap<>(Priority.class);

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
		this.allTasks.add(task);
		switch (task.getPriority()) {
		case HIGH:
			this.prioHighTasks.add(task);
			break;
		case LOW:
			this.prioLowTasks.add(task);
			break;
		case ONCE:
			this.prioOnceTasks.add(task);
			// Fill the 'nextOnceTasks'. This happens only once.
			this.nextOnceTasks.add(task);
			break;
		}
	}

	/**
	 * Removes a Task.
	 *
	 * @param task the Task
	 */
	public synchronized void removeTask(T task) {
		this.allTasks.remove(task);
		switch (task.getPriority()) {
		case HIGH:
			this.prioHighTasks.remove(task);
			break;
		case LOW:
			this.prioLowTasks.remove(task);
			this.nextLowTasks.remove(task);
			break;
		case ONCE:
			this.prioOnceTasks.remove(task);
			this.nextOnceTasks.remove(task);
			break;
		}
	}

	/**
	 * Clears all Tasks lists.
	 */
	public synchronized void clearAll() {
		this.allTasks.clear();
		this.prioHighTasks.clear();
		this.prioLowTasks.clear();
		this.nextLowTasks.clear();
		this.prioOnceTasks.clear();
		this.nextOnceTasks.clear();
	}

	/**
	 * Get all tasks with the given Priority.
	 *
	 * @param priority the Priority
	 * @return a list of Tasks
	 */
	public synchronized List<T> getAllTasks(Priority priority) {
		switch (priority) {
		case HIGH:
			return Collections.unmodifiableList(this.prioHighTasks);
		case LOW:
			return Collections.unmodifiableList(this.prioLowTasks);
		case ONCE:
			return Collections.unmodifiableList(this.prioOnceTasks);
		}
		assert true;
		return new ArrayList<>();
	}

	/**
	 * Gets the next Tasks. This should normally be called once per Cycle.
	 *
	 * @return a list of Tasks.
	 */
	public synchronized List<T> getNextTasks() {
		List<T> result = new ArrayList<>(this.prioHighTasks);
		/*
		 * Handle LOW
		 */
		if (this.nextLowTasks.isEmpty()) {
			// Refill the 'nextLowTasks'. This happens every time the list is empty.
			this.nextLowTasks.addAll(this.prioLowTasks);
		}
		var task = this.nextLowTasks.poll();
		if (task != null) {
			result.add(task);
		}

		/*
		 * Handle ONCE
		 */
		task = this.nextOnceTasks.poll();
		if (task != null) {
			result.add(task);
		}
		return Collections.unmodifiableList(result);
	}

	/**
	 * Gets all Tasks.
	 *
	 * @return a list of all Tasks.
	 */
	public synchronized List<T> getAllTasks() {
		return Collections.unmodifiableList(this.allTasks);
	}

	/**
	 * Gets tasks sequentially.
	 *
	 * @return the next task; null if there are no tasks
	 */
	public synchronized T getOneTask() {
		if (this.allTasks.isEmpty()) {
			return null;
		}
		if (this.nextTaskIndex > this.allTasks.size() - 1) {
			// start over
			this.nextTaskIndex = 0;
		}
		return this.allTasks.get(this.nextTaskIndex++);
	}

	/**
	 * Gets one task that is lower than the given Priority sequentially.
	 *
	 * @return the next task; null if there are no tasks with the given Priority
	 */
	public synchronized T getOneTask(Priority priority) {
		var tasks = this.getAllTasks(priority);
		if (tasks.isEmpty()) {
			return null;
		}
		var nextTaskIndex = this.nextTaskIndexPerPriority.get(priority);
		if (nextTaskIndex == null) {
			// start new
			nextTaskIndex = 0;
		}
		// reached end of list -> start over?
		if (nextTaskIndex > tasks.size() - 1) {
			switch (priority) {
			case HIGH:
			case LOW:
				// start over
				nextTaskIndex = 0;
				break;
			case ONCE:
				// do not start over
				return null;
			}
		}

		this.nextTaskIndexPerPriority.put(priority, nextTaskIndex + 1);
		return tasks.get(nextTaskIndex);
	}

}