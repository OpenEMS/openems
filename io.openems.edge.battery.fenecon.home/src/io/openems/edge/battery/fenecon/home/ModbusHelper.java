package io.openems.edge.battery.fenecon.home;

import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;

public interface ModbusHelper {

	/**
	 * Get the {@link BridgeModbus}.
	 * 
	 * @return the {@link BridgeModbus}
	 */
	public BridgeModbus getModbus();

	/**
	 * Get defined {@link ModbusProtocol}.
	 * 
	 * @return the {@link ModbusProtocol}
	 */
	public ModbusProtocol getDefinedModbusProtocol();

}
