package io.openems.edge.bridge.modbus.api.task;

import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.common.taskmanager.ManagedTask;

public interface Task extends ManagedTask {

	/**
	 * Gets the ModbusElements
	 * 
	 * @return
	 */
	ModbusElement<?>[] getElements();

	/**
	 * Gets the start modbus register address
	 * 
	 * @return
	 */
	int getStartAddress();

	/**
	 * Sets the parent
	 */
	void setParent(AbstractOpenemsModbusComponent parent);

}