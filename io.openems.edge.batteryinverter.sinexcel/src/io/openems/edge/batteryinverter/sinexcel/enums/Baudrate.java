package io.openems.edge.batteryinverter.sinexcel.enums;

import io.openems.common.types.OptionsEnum;

//default 0
public enum Baudrate implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	B_19200(0, "19200"), //
	B_9600(1, "9600"); //

	private final int value;
	private final String name;

	private Baudrate(int value, String name) {
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