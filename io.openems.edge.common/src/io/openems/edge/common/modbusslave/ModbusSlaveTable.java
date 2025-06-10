package io.openems.edge.common.modbusslave;

public class ModbusSlaveTable {

	private final ModbusSlaveNatureTable[] natureTables;
	private final int length;

	public ModbusSlaveTable(ModbusSlaveNatureTable... natureTables) {
		this.natureTables = natureTables;

		// calculate total length
		var length = 0;
		for (ModbusSlaveNatureTable natureTable : natureTables) {
			length += natureTable.getLength();
		}
		this.length = length;
	}

	public int getLength() {
		return this.length;
	}

	public ModbusSlaveNatureTable[] getNatureTables() {
		return this.natureTables;
	}
}
