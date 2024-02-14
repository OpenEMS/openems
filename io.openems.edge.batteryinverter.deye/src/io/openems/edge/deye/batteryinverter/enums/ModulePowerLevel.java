package io.openems.edge.deye.batteryinverter.enums;

import io.openems.common.types.OptionsEnum;

public enum ModulePowerLevel implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	TEN_KW(0, " 10 kW"), //
	TWENTY_KW(1, "20 kW"), //
	THIRTY_KW(2, "30 kW"), //
	TWENTY_NINE_KW(3, "29 kW"); //

	private final int value;
	private final String name;

	private ModulePowerLevel(int value, String name) {
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