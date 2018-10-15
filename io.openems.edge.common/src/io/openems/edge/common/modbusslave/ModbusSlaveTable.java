package io.openems.edge.common.modbusslave;

public class ModbusSlaveTable {

	private final String componentId;
	private final ModbusSlaveNatureTable[] natureTables;

	public ModbusSlaveTable(String componentId, ModbusSlaveNatureTable... natureTables) {
		this.componentId = componentId;
		this.natureTables = natureTables;
	}

}
