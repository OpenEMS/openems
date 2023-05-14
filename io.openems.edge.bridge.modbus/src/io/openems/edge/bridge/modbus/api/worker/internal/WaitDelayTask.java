package io.openems.edge.bridge.modbus.api.worker.internal;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.bridge.modbus.api.AbstractModbusBridge;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.bridge.modbus.api.task.Task;
import io.openems.edge.common.taskmanager.Priority;

public class WaitDelayTask implements Task {

	private final Logger log = LoggerFactory.getLogger(WaitDelayTask.class);
	private final Runnable onFinished;
	private long delay;

	private AbstractOpenemsModbusComponent parent = null;

	public WaitDelayTask(long delay, Runnable onFinished) {
		this.delay = delay;
		this.onFinished = onFinished;
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
	public <T> int execute(AbstractModbusBridge bridge) {
		if (this.delay > 0) {
			var start = Instant.now();
			try {
				Thread.sleep(this.delay);

			} catch (InterruptedException e) {
				this.log.info("WaitDelayTask interrupted: " + e.getMessage());

			} finally {
				this.delay -= Duration.between(start, Instant.now()).toMillis();
			}
		}

		if (this.delay <= 0) {
			this.onFinished.run();
		}

		return 0;
	}

	@Override
	public String toString() {
		return "WaitDelayTask [delay=" + this.delay + "]";
	}
}
