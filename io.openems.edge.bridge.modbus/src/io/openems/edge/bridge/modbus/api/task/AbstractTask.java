package io.openems.edge.bridge.modbus.api.task;

import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractModbusBridge;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.LogVerbosity;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;

/**
 * An abstract Modbus 'AbstractTask' is holding references to one or more Modbus
 * {@link AbstractModbusElement} which have register addresses in the same
 * range.
 */
public abstract class AbstractTask implements Task {

	private static final long DEFAULT_EXECUTION_DURATION = 300;

	private final int length;
	private final int startAddress;
	private final Stopwatch stopwatch = Stopwatch.createUnstarted();

	private ModbusElement<?>[] elements;
	private AbstractOpenemsModbusComponent parent = null; // this is always set by ModbusProtocol.addTask()
	private boolean hasBeenExecutedSuccessfully = false;
	private long lastExecuteDuration = DEFAULT_EXECUTION_DURATION; // initialize to some default

	public AbstractTask(int startAddress, AbstractModbusElement<?>... elements) {
		this.startAddress = startAddress;
		this.elements = elements;
		for (AbstractModbusElement<?> element : elements) {
			element.setModbusTask(this);
		}
		int length = 0;
		for (AbstractModbusElement<?> element : elements) {
			length += element.getLength();
		}
		this.length = length;
	}

	public ModbusElement<?>[] getElements() {
		return elements;
	}

	public int getLength() {
		return length;
	}

	public int getStartAddress() {
		return startAddress;
	}

	public void setParent(AbstractOpenemsModbusComponent parent) {
		this.parent = parent;
	}

	@Override
	public AbstractOpenemsModbusComponent getParent() {
		return parent;
	}

	/**
	 * Executes the tasks - i.e. sends the query of a ReadTask or writes a
	 * WriteTask.
	 * 
	 * <p>
	 * Internally the _execute()-method of the specific subclass gets called.
	 * 
	 * @param bridge the Modbus-Bridge
	 * @return the number of executed Sub-Tasks
	 * @throws OpenemsException on error
	 */
	public final synchronized int execute(AbstractModbusBridge bridge) throws OpenemsException {
		this.stopwatch.reset();
		this.stopwatch.start();
		try {
			int noOfSubTasksExecuted = this._execute(bridge);

			// no exception -> mark this task as successfully executed
			this.hasBeenExecutedSuccessfully = true;
			return noOfSubTasksExecuted;

		} finally {
			this.lastExecuteDuration = this.stopwatch.elapsed(TimeUnit.MILLISECONDS);
		}
	}

	protected abstract int _execute(AbstractModbusBridge bridge) throws OpenemsException;

	/*
	 * Enable Debug mode for this Element. Activates verbose logging. TODO:
	 * implement debug write in all implementations (FC16 is already done)
	 */
	private boolean isDebug = false;

	public AbstractTask debug() {
		this.isDebug = true;
		return this;
	}

	public boolean isDebug() {
		return isDebug;
	}

	/**
	 * Combines the global and local (via {@link #isDebug} log verbosity.
	 * 
	 * @param bridge the parent Bridge
	 * @return the combined LogVerbosity
	 */
	protected LogVerbosity getLogVerbosity(AbstractModbusBridge bridge) {
		if (this.isDebug) {
			return LogVerbosity.READS_AND_WRITES;
		}
		return bridge.getLogVerbosity();
	}

	@Override
	public boolean hasBeenExecuted() {
		return this.hasBeenExecutedSuccessfully;
	}

	@Override
	public void deactivate() {
		for (ModbusElement<?> element : this.elements) {
			element.deactivate();
		}
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(this.getActiondescription());
		sb.append(" [");
		sb.append(this.parent.id());
		sb.append(";unitid=");
		sb.append(this.parent.getUnitId());
		sb.append(";ref=");
		sb.append(this.getStartAddress());
		sb.append("/0x");
		sb.append(Integer.toHexString(this.getStartAddress()));
		sb.append(";length=");
		sb.append(this.getLength());
		sb.append("]");
		return sb.toString();
	}

	public long getExecuteDuration() {
		return lastExecuteDuration;
	}

	protected abstract String getActiondescription();
}
