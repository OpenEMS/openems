package io.openems.edge.kostal.piko.core.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import io.openems.common.worker.AbstractCycleWorker;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.taskmanager.TasksManager;

public class Worker extends AbstractCycleWorker {

	private final Protocol protocol;
	private final TasksManager<ReadTask> readTasksManager;

	public Worker(Protocol protocol, TasksManager<ReadTask> readTasksManager) {
		this.protocol = protocol;
		this.readTasksManager = readTasksManager;
	}

	@Override
	protected void forever() {
		var nextReadTasks = this.getNextTasks();
		this.protocol.execute(nextReadTasks);
	}

	private final Queue<ReadTask> nextLowTasks = new LinkedList<>();

	/**
	 * Gets the next Tasks. This should normally be called once per Cycle.
	 *
	 * @return a list of Tasks.
	 */
	protected List<ReadTask> getNextTasks() {
		List<ReadTask> result = new ArrayList<>(this.readTasksManager.getTasks(Priority.HIGH));
		/*
		 * Handle LOW
		 */
		if (this.nextLowTasks.isEmpty()) {
			// Refill the 'nextLowTasks'. This happens every time the list is empty.
			this.nextLowTasks.addAll(this.readTasksManager.getTasks(Priority.LOW));
		}
		var task = this.nextLowTasks.poll();
		if (task != null) {
			result.add(task);
		}

		return Collections.unmodifiableList(result);
	}
}
