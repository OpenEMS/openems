package io.openems.edge.common.modbusslave;

import io.openems.edge.common.component.OpenemsComponent;

public interface ModbusSlave extends OpenemsComponent {

	public ModbusSlaveTable getModbusSlaveTable();

}
