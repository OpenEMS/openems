package io.openems.edge.bridge.modbus.api.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractModbusBridge;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.common.taskmanager.Priority;

public class WaitTask implements Task {

	private final Logger log = LoggerFactory.getLogger(WaitTask.class);
	private final long delay;

	private AbstractOpenemsModbusComponent parent = null;

	public WaitTask(long delay) {
		this.delay = delay;
	}

	@Override
	public Priority getPriority() {
		return Priority.LOW;
	}

	@Override
	public ModbusElement<?>[] getElements() {
		return new ModbusElement[0];
	}

	@Override
	public int getStartAddress() {
		return 0;
	}

	@Override
	public int getLength() {
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
	public <T> int execute(AbstractModbusBridge bridge) throws OpenemsException {
		try {
			Thread.sleep(this.delay);
		} catch (InterruptedException e) {
			this.log.warn(e.getMessage());
		}
		return 0;
	}

	@Override
	public boolean hasBeenExecuted() {
		return true;
	}

	@Override
	public long getExecuteDuration() {
		return this.delay;
	}

}