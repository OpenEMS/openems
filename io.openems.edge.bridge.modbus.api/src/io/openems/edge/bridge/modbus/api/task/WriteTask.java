package io.openems.edge.bridge.modbus.api.task;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster;

public interface WriteTask {
	/**
	 * Executes writing for this Task to the Modbus device
	 * 
	 * @param master
	 * @param unitId
	 * @throws ModbusException
	 */
	public abstract void executeWrite(ModbusTCPMaster master) throws ModbusException;
}
