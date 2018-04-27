package io.openems.edge.bridge.modbus.api.task;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster;

import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;

/**
 * A Modbus 'ReadTask' is holding references to one or more Modbus
 * {@link AbstractModbusElement} which have register addresses in the same
 * range. The ReadTask handles the execution (query) on this range. @{link
 * WriteTask} inherits from ReadTask.
 * 
 * @author stefan.feilmeier
 */
public interface ReadTask {

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
	 * Sends a query for this Task to the Modbus device
	 * 
	 * @param master
	 * @param unitId
	 * @throws ModbusException
	 */
	public abstract void executeQuery(ModbusTCPMaster master) throws ModbusException;
}
