package io.openems.edge.bridge.modbus;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.bridge.modbus.api.task.Task;
import io.openems.edge.common.taskmanager.Priority;

public class MarkerTask implements Task {

	private AbstractOpenemsModbusComponent parent = null;

	public MarkerTask() {
	}

	@Override
	public Priority getPriority() {
		return Priority.ONCE;
	}

	@Override
	public ModbusElement<?>[] getElements() {
		return new ModbusElement<?>[0];
	}

	@Override
	public int getStartAddress() {
		return 0;
	}

	@Override
	public void setParent(AbstractOpenemsModbusComponent parent) {
		this.parent = parent;
	}

	@Override
	public AbstractOpenemsModbusComponent getParent() {
		return this.parent;
	}

	@Override
	public void deactivate() {
	}

	@Override
	public <T> void execute(AbstractModbusBridge bridge) throws OpenemsException {
	}

	@Override
	public String toString() {
		return "MarkerTask";
	}

}
