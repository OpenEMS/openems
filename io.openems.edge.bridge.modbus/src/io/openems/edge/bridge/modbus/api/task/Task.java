package io.openems.edge.bridge.modbus.api.task;

import io.openems.edge.bridge.modbus.api.element.ModbusElement;

public interface Task {

	/**
	 * Sets the modbus unit id
	 * 
	 * @param unitId
	 */
	void setUnitId(int unitId);

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

}