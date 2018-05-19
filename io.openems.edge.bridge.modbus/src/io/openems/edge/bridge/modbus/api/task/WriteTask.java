package io.openems.edge.bridge.modbus.api.task;

import com.ghgande.j2mod.modbus.ModbusException;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.AbstractModbusBridge;
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
	 * @param bridge
	 * @param unitId
	 * @throws ModbusException
	 */
	public abstract void executeWrite(AbstractModbusBridge bridge) throws OpenemsException;
}
