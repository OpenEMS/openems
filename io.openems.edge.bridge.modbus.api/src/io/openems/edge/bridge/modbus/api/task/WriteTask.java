package io.openems.edge.bridge.modbus.api.task;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster;

import io.openems.edge.bridge.modbus.api.element.ModbusElement;

public interface WriteTask {

	/**
	 * Sets the modbus unit id
	 * 
	 * @param unitId
	 */
	public void setUnitId(int unitId);

	/**
	 * Gets the ModbusElements
	 * 
	 * @return
	 */
	public ModbusElement<?>[] getElements();

	/**
	 * Gets the start modbus register address
	 * 
	 * @return
	 */
	public int getStartAddress();

	/**
	 * Executes writing for this Task to the Modbus device
	 * 
	 * @param master
	 * @param unitId
	 * @throws ModbusException
	 */
	public abstract void executeWrite(ModbusTCPMaster master) throws ModbusException;
}
