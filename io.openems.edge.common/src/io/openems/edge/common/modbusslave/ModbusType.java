package io.openems.edge.common.modbusslave;

public enum ModbusType {
	ENUM16(1, "enum16"), //
	UINT16(1, "uint16"), //
	UINT32(2, "uint32"), //
	UINT64(4, "uint64"), //
	FLOAT32(2, "float32"), //
	FLOAT64(4, "float64"), //
	STRING16(16, "string16");

	private final int words;
	private final String name;

	private ModbusType(int words, String name) {
		this.words = words;
		this.name = name;
	}

	public int getWords() {
		return this.words;
	}

	@Override
	public String toString() {
		return this.name;
	}
}