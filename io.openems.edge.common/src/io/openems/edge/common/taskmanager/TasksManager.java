package io.openems.edge.common.taskmanager;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class TasksManager<T extends Task> {

	private final List<T> prioHighTasks = new ArrayList<>();
	private final List<T> prioLowTasks = new ArrayList<>();
	private final List<T> prioOnceTasks = new ArrayList<>();

	private final Queue<T> nextLowTasks = new LinkedList<>();
	private final Queue<T> nextOnceTasks = new LinkedList<>();

	@SafeVarargs
	public TasksManager(T... tasks) {
		for (T task : tasks) {
			switch (task.getPriority()) {
			case HIGH:
				this.prioHighTasks.add(task);
				break;
			case LOW:
				this.prioLowTasks.add(task);
				break;
			case ONCE:
				this.prioOnceTasks.add(task);
				break;
			}
		}
		// Fill the 'nextOnceTasks'. This happens only once.
		this.nextOnceTasks.addAll(prioOnceTasks);
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
		return result;
	}
}