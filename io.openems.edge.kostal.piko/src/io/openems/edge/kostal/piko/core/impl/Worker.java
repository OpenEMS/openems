package io.openems.edge.kostal.piko.core.impl;

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
		var nextReadTasks = this.readTasksManager.getNextTasks();
		this.protocol.execute(nextReadTasks);
	}
}
