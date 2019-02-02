package io.openems.edge.bridge.modbus.api.task;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.AbstractModbusBridge;
import io.openems.edge.common.taskmanager.ManagedTask;
import io.openems.edge.common.taskmanager.Priority;

public interface WriteTask extends Task, ManagedTask {

	/**
	 * Executes writing for this WriteTask to the Modbus device.
	 * 
	 * @param bridge the AbstractModbusBridge
	 * @throws OpenemsException on error
	 */
	public abstract void executeWrite(AbstractModbusBridge bridge) throws OpenemsException;

	/**
	 * Priority for WriteTasks is by default always HIGH.
	 * 
	 * @return the Priority
	 */
	public default Priority getPriority() {
		return Priority.HIGH;
	}
}
