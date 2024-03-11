package io.openems.edge.bridge.modbus.api.task;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.bridge.modbus.api.AbstractModbusBridge;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.common.taskmanager.Priority;

public abstract non-sealed class WaitTask implements Task {

	private final Logger log = LoggerFactory.getLogger(WaitTask.class);
	private AbstractOpenemsModbusComponent parent = null;

	private int skipCycles = 0;

	@Override
	public final void setParent(AbstractOpenemsModbusComponent parent) {
		this.parent = parent;
	}

	@Override
	public final AbstractOpenemsModbusComponent getParent() {
		return this.parent;
	}

	@Override
	public final Priority getPriority() {
		return Priority.LOW;
	}

	@Override
	public final ModbusElement[] getElements() {
		return new ModbusElement[0];
	}

	@Override
	public final int getStartAddress() {
		return 0;
	}

	@Override
	public final int getLength() {
		return 0;
	}

	@Override
	public WaitTask setSkipCycles(int cycles) {
		this.skipCycles = cycles;
		return this;
	}

	@Override
	public int getSkipCycles() {
		return this.skipCycles;
	}

	@Override
	public final void deactivate() {
	}

	@Override
	public final ExecuteState execute(AbstractModbusBridge bridge) {
		try {
			this._execute();
		} catch (InterruptedException e) {
			this.log.info(this.toString() + " interrupted: " + e.getMessage());
		}
		return ExecuteState.NO_OP;
	}

	protected abstract void _execute() throws InterruptedException;

	public static class Mutex extends WaitTask {

		private final io.openems.common.utils.Mutex mutex = new io.openems.common.utils.Mutex(false);

		/** Release the Mutex, i.e. interrupt waiting. */
		public void release() {
			this.mutex.release();
		}

		@Override
		protected void _execute() throws InterruptedException {
			this.mutex.awaitOrTimeout(0, TimeUnit.MILLISECONDS); // throw away active release
			this.mutex.await();
		}

		@Override
		public String toString() {
			return "WaitTask.Mutex []";
		}
	}

	public static class Delay extends WaitTask {

		public final long initialDelay;

		private final Runnable onFinished;

		private long delay;

		public Delay(long delay, Runnable onFinished) {
			this.initialDelay = this.delay = delay;
			this.onFinished = onFinished;
		}

		@Override
		protected void _execute() throws InterruptedException {
			var start = Instant.now();
			try {
				if (this.delay > 0) {
					Thread.sleep(this.delay);
				}
			} finally {
				this.delay -= Duration.between(start, Instant.now()).toMillis();

				if (this.delay <= 0) {
					this.onFinished.run();
				}
			}
		}

		@Override
		public String toString() {
			return "WaitDelayTask [delay=" + this.delay + "]";
		}
	}
}
