package io.openems.edge.kostal.piko.core.impl;

import java.util.List;

import io.openems.common.worker.AbstractCycleWorker;
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
		List<ReadTask> nextReadTasks = this.readTasksManager.getNextTasks();
		this.protocol.execute(nextReadTasks);
	}
}
