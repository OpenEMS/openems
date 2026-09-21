package io.openems.edge.protectionrelay.telehaase.na003m64.enums;

import io.openems.common.types.OptionsEnum;

public enum ModbusProperties implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	EVEN_PARITY(0, "8E1; 8 data bits, even parity, 1 stop bit (default)"), //
	ODD_PARITY(1, "8O1; 8 data bits, odd parity, 1 stop bit"), //
	NO_PARITY_2(2, "8N2; 8 data bits, no parity, 2 stop bit"), //
	NO_PARITY_1(3, "8N1; 8 data bits, no parity, 1 stop bit (NOT conform)"), //
	;

	private final int value;
	private final String name;

	ModbusProperties(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}
