package io.openems.edge.batteryinverter.sinexcel.enums;

import io.openems.common.types.OptionsEnum;

public enum ProtocolSelection implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	MODBUS(0, "Modbus"), //
	SUNSPEC(1, "Sunspec"); //

	private final int value;
	private final String name;

	private ProtocolSelection(int value, String name) {
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