package io.openems.edge.bridge.can.api.task;

import io.openems.edge.common.taskmanager.ManagedTask;
import io.openems.edge.common.taskmanager.Priority;

public interface WriteTask extends Task, ManagedTask {

	/**
	 * Priority for WriteTasks is by default always HIGH.
	 *
	 * @return the Priority
	 */
	@Override
	public default Priority getPriority() {
		return Priority.HIGH;
	}
}
