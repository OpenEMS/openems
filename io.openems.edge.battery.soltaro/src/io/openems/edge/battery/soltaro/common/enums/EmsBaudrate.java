package io.openems.edge.battery.soltaro.common.enums;

import io.openems.common.types.OptionsEnum;

public enum EmsBaudrate implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	BAUDRATE_9600(1, "Baudrate 9600"), //
	BAUDRATE_19200(3, "Baudrate 19200"), //
	BAUDRATE_57600(4, "Baudrate 57600"); //

	private final int value;
	private final String name;

	private EmsBaudrate(int value, String name) {
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
