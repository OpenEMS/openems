package io.openems.edge.bridge.modbus.api.task;

import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.common.taskmanager.ManagedTask;

public interface Task extends ManagedTask {

	/**
	 * Gets the ModbusElements.
	 * 
	 * @return an array of ModbusElements
	 */
	ModbusElement<?>[] getElements();

	/**
	 * Gets the start Modbus register address.
	 * 
	 * @return the address
	 */
	int getStartAddress();

	/**
	 * Sets the parent.
	 * 
	 * @param parent the parent {@link AbstractOpenemsModbusComponent}.
	 */
	void setParent(AbstractOpenemsModbusComponent parent);

}