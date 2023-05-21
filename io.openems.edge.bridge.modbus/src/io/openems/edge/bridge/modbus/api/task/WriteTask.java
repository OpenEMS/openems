package io.openems.edge.bridge.modbus.api.task;

import io.openems.edge.common.taskmanager.Priority;

public non-sealed interface WriteTask extends Task {
	// TODO add IMMEDIATE priority for WriteTasks. See
	// https://github.com/OpenEMS/openems/pull/1976#issuecomment-1411609673
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
