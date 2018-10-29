package io.openems.edge.common.modbusslave;

public enum ModbusType {
	UINT16(1), //
	FLOAT32(2), //
	STRING16(16);

	private final int words;

	private ModbusType(int words) {
		this.words = words;
	}

	public int getWords() {
		return words;
	}
}