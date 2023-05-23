package io.openems.edge.bridge.modbus.api.worker.internal;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.Mutex;
import io.openems.edge.bridge.modbus.api.AbstractModbusBridge;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.bridge.modbus.api.task.WaitTask;
import io.openems.edge.common.taskmanager.Priority;

public class WaitMutexTask implements WaitTask {

	private final Logger log = LoggerFactory.getLogger(WaitMutexTask.class);
	private final Mutex mutex = new Mutex(false);

	private AbstractOpenemsModbusComponent parent = null;

	/**
	 * Release the Mutex, i.e. interrupt waiting.
	 */
	public void release() {
		this.mutex.release();
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
			this.mutex.awaitOrTimeout(0, TimeUnit.MILLISECONDS); // throw away active release
			this.mutex.await();

		} catch (InterruptedException e) {
			this.log.info("WaitMutexTask interrupted: " + e.getMessage());
		}

		return 0;
	}

	@Override
	public String toString() {
		return "WaitMutexTask []";
	}
}
