package io.openems.edge.common.taskmanager;

import java.util.ArrayList;
import java.util.Collections;
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
}