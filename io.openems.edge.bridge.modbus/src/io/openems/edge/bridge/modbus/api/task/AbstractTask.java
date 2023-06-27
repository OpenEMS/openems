package io.openems.edge.bridge.modbus.api.task;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractModbusBridge;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.LogVerbosity;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;

/**
 * An abstract Modbus 'AbstractTask' is holding references to one or more Modbus
 * {@link ModbusElement} which have register addresses in the same range.
 */
public abstract class AbstractTask {

	protected final String name;
	protected final int startAddress;
	protected final int length;
	protected final ModbusElement<?>[] elements;

	private AbstractOpenemsModbusComponent parent = null; // this is always set by ModbusProtocol.addTask()

	public AbstractTask(String name, int startAddress, ModbusElement<?>... elements) {
		this.name = name;
		this.startAddress = startAddress;
		this.elements = elements;
		for (var element : elements) {
			element.setModbusTask(this);
		}
		var length = 0;
		for (var element : elements) {
			length += element.getLength();
		}
		this.length = length;
	}

	// Override for Task.getElements()
	public ModbusElement<?>[] getElements() {
		return this.elements;
	}

	// Override for Task.getLength()
	public int getLength() {
		return this.length;
	}

	// Override for Task.getStartAddress()
	public int getStartAddress() {
		return this.startAddress;
	}

	public void setParent(AbstractOpenemsModbusComponent parent) {
		this.parent = parent;
	}

	public AbstractOpenemsModbusComponent getParent() {
		return this.parent;
	}

	/**
	 * Executes the tasks - i.e. sends the query of a ReadTask or writes a
	 * WriteTask.
	 *
	 * @param bridge the Modbus-Bridge
	 * @return the number of executed Sub-Tasks
	 * @throws OpenemsException on error
	 */
	public abstract int execute(AbstractModbusBridge bridge) throws OpenemsException;

	/*
	 * Enable Debug mode for this Element. Activates verbose logging. TODO:
	 * implement debug write in all implementations (FC16 is already done)
	 */
	private boolean isDebug = false;

	/**
	 * Activate Debug-Mode.
	 * 
	 * @return myself
	 */
	public AbstractTask debug() {
		this.isDebug = true;
		return this;
	}

	public boolean isDebug() {
		return this.isDebug;
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

	/**
	 * Deactivate.
	 */
	public void deactivate() {
		for (ModbusElement<?> element : this.elements) {
			element.deactivate();
		}
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		sb.append(this.name);
		sb.append(" [");
		sb.append(this.parent.id());
		sb.append(";unitid=");
		sb.append(this.parent.getUnitId());
		sb.append(";ref=");
		sb.append(this.startAddress);
		sb.append("/0x");
		sb.append(Integer.toHexString(this.startAddress));
		sb.append(";length=");
		sb.append(this.length);
		sb.append("]");
		return sb.toString();
	}
}
