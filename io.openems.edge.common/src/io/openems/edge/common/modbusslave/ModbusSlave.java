package io.openems.edge.common.modbusslave;

import io.openems.common.channel.AccessMode;
import io.openems.edge.common.component.OpenemsComponent;

public interface ModbusSlave extends OpenemsComponent {

	/**
	 * Gets the Modbus-Slave-Table for this OpenEMS-Component.
	 *
	 * @param accessMode filters the Modbus-Records that should be shown
	 * @return the Modbus-Slave-Table
	 */
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode);

}
