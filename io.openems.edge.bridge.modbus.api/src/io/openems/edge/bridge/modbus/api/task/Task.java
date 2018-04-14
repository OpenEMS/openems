package io.openems.edge.bridge.modbus.api.task;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster;

import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;

/**
 * A Modbus 'Task' is holding references to one or more Modbus
 * {@link AbstractModbusElement} which have register addresses in the same
 * range. The Task handles the execution (query, write,...) on this range.
 * 
 * @author stefan.feilmeier
 */
public abstract class Task {

	private final int length;
	private final int startAddress;

	private ModbusElement[] elements;
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

	public ModbusElement[] getElements() {
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

	/**
	 * Sends a query for this Task to the Modbus device
	 * 
	 * @param master
	 * @param unitId
	 * @throws ModbusException
	 */
	public abstract void executeQuery(ModbusTCPMaster master) throws ModbusException;

	@Override
	public String toString() {
		return "Task [startAddress=" + startAddress + ", length=" + length + "]";
	}
}
