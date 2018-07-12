package io.openems.edge.ess.kostal.piko;

import java.util.List;

import io.openems.edge.common.worker.AbstractCycleWorker;

public class Worker extends AbstractCycleWorker {

	private final Protocol protocol;
	private final ReadTasksManager readTasksManager;

	public Worker(Protocol protocol, ReadTasksManager readTasksManager) {
		this.protocol = protocol;
		this.readTasksManager = readTasksManager;
	}

	@Override
	protected void forever() {
		List<ReadTask> nextReadTasks = this.readTasksManager.getNextReadTasks();
		this.protocol.execute(nextReadTasks);
	}
}
