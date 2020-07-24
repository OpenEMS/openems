package io.openems.edge.battery.soltaro.single.versionb.enums;

import io.openems.common.types.OptionsEnum;

public enum ContactExport implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	HIGH(0x1, "High"), //
	LOW(0x2, "Low");

	private final int value;
	private final String name;

	private ContactExport(int value, String name) {
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