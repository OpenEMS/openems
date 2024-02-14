package io.openems.edge.deye.batteryinverter.enums;

import io.openems.common.types.OptionsEnum;

public enum Epo implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	INVALID(0, "Modbus"), //
	EPO(1, "Sunspec"), //
	DRMO(2, "Sunspec");//

	private final int value;
	private final String name;

	private Epo(int value, String name) {
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