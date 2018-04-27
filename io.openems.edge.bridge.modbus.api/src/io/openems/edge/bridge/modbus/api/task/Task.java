package io.openems.edge.bridge.modbus.api.task;

import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;

/**
 * An abstract Modbus 'Task' is holding references to one or more Modbus
 * {@link AbstractModbusElement} which have register addresses in the same
 * range.
 * 
 * @author stefan.feilmeier
 */
public abstract class Task {

	private final int length;
	private final int startAddress;

	private ModbusElement<?>[] elements;
	private int unitId; // this is always set by ModbusProtocol.addTask()

	public Task(int startAddress, AbstractModbusElement<?>... elements) {
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

	public void setUnitId(int unitId) {
		this.unitId = unitId;
	}

	public int getUnitId() {
		return unitId;
	}

	@Override
	public abstract String toString();
}
