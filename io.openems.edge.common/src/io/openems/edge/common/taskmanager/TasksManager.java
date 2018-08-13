package io.openems.edge.common.taskmanager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;

public class TasksManager<T extends Task> {

	private final List<T> allTasks = new CopyOnWriteArrayList<>();

	private final List<T> prioHighTasks = new CopyOnWriteArrayList<>();
	private final List<T> prioLowTasks = new CopyOnWriteArrayList<>();
	private final List<T> prioOnceTasks = new CopyOnWriteArrayList<>();

	private final Queue<T> nextLowTasks = new LinkedList<>();
	private final Queue<T> nextOnceTasks = new LinkedList<>();

	@SafeVarargs
	public TasksManager(T... tasks) {
		this.addTasks(tasks);
	}

	@SafeVarargs
	public final synchronized void addTasks(T... tasks) {
		for (T task : tasks) {
			this.addTask(task);
		}
	}
		
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

	public synchronized List<T> getNextReadTasks() {
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

	public synchronized List<T> getAllTasks() {
		return Collections.unmodifiableList(this.allTasks);
	}
}