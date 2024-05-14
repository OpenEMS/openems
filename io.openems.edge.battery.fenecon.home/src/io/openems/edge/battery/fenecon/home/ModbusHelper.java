package io.openems.edge.battery.fenecon.home;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;

public interface ModbusHelper {

	/**
	 * Get modbus bridge.
	 * 
	 * @return modbus bridge.
	 */
	public BridgeModbus getModbus();

	/**
	 * Get defined modbus protocol.
	 * 
	 * @return modbus protocol
	 * @throws OpenemsException on error
	 */
	public ModbusProtocol getDefinedModbusProtocol() throws OpenemsException;

}
