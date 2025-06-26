package io.openems.edge.batteryinverter.sinexcel.enums;

import io.openems.common.types.OptionsEnum;

public enum InterfaceType implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	RS_485(0, "RS-485"), //
	ETHERNET(1, "Ethernet"); //

	private final int value;
	private final String name;

	private InterfaceType(int value, String name) {
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