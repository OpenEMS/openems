package io.openems.edge.bridge.modbus.api.task;

import io.openems.edge.bridge.modbus.AbstractModbusBridge;
import io.openems.edge.bridge.modbus.LogVerbosity;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;

/**
 * An abstract Modbus 'AbstractTask' is holding references to one or more Modbus
 * {@link AbstractModbusElement} which have register addresses in the same
 * range.
 */
public abstract class AbstractTask implements Task {

	private final int length;
	private final int startAddress;

	private ModbusElement<?>[] elements;
	private AbstractOpenemsModbusComponent parent = null; // this is always set by ModbusProtocol.addTask()

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

	protected abstract String getActiondescription();
}
