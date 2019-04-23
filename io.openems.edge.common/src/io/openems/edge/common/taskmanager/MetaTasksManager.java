package io.openems.edge.common.taskmanager;

import java.util.ArrayList;
import java.util.Arrays;
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
public class MetaTasksManager<T extends ManagedTask> {

	private final List<TasksManager<T>> tasksManagers = new ArrayList<>();

	private final Queue<T> nextHighTasks = new LinkedList<>();
	private final Queue<T> nextLowTasks = new LinkedList<>();
	private final Queue<T> nextOnceTasks = new LinkedList<>();

	private int nextTaskIndex = 0;
	private EnumMap<Priority, Integer> nextTaskIndexPerPriority = new EnumMap<>(Priority.class);

	@SafeVarargs
	public MetaTasksManager(TasksManager<T>... tasksManagers) {
		this.addTasksManagers(tasksManagers);
	}

	/**
	 * Adds multiple Tasks.
	 * 
	 * @param tasks an array of Tasks
	 */
	@SafeVarargs
	public final synchronized void addTasksManagers(TasksManager<T>... tasksManagers) {
		for (TasksManager<T> tasksManager : tasksManagers) {
			this.addTasksManager(tasksManager);
		}
	}

	/**
	 * Adds multiple Tasks.
	 * 
	 * @param tasks an array of Tasks
	 */
	public void addTasksManagers(List<TasksManager<T>> tasksManagers) {
		for (TasksManager<T> tasksManager : tasksManagers) {
			this.addTasksManager(tasksManager);
		}
	}

	/**
	 * Adds a Task, taking its Priority in consideration.
	 * 
	 * @param task the Task
	 */
	public synchronized void addTasksManager(TasksManager<T> tasksManager) {
		this.tasksManagers.add(tasksManager);
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
	public synchronized List<T> getTasks(Priority priority) {
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
		List<T> result = new ArrayList<>();
		/*
		 * Handle HIGH
		 */
		result.addAll(this.prioHighTasks);

		/*
		 * Handle LOW
		 */
		if (nextLowTasks.isEmpty()) {
			// Refill the 'nextLowTasks'. This happens every time the list is empty.
			this.nextLowTasks.addAll(prioLowTasks);
		}
		T task = nextLowTasks.poll();
		if (task != null) {
			result.add(task);
		}

		/*
		 * Handle ONCE
		 */
		task = nextOnceTasks.poll();
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
		List<T> tasks = this.getTasks(priority);
		if (tasks.isEmpty()) {
			return null;
		}
		Integer nextTaskIndex = this.nextTaskIndexPerPriority.get(priority);
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