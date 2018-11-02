package io.openems.edge.common.modbusslave;

public enum ModbusType {
	UINT16(1, "uint16"), //
	FLOAT32(2, "float32"), //
	STRING16(16, "string16");

	private final int words;
	private final String name;

	private ModbusType(int words, String name) {
		this.words = words;
		this.name = name;
	}

	public int getWords() {
		return words;
	}

	public String toString() {
		return name;
	}
}