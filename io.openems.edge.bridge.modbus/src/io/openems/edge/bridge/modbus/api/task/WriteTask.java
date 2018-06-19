package io.openems.edge.bridge.modbus.api.task;

import com.ghgande.j2mod.modbus.ModbusException;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.AbstractModbusBridge;

public interface WriteTask extends Task {

	/**
	 * Executes writing for this AbstractTask to the Modbus device
	 * 
	 * @param bridge
	 * @param unitId
	 * @throws ModbusException
	 */
	public abstract void executeWrite(AbstractModbusBridge bridge) throws OpenemsException;
}
