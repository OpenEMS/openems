package io.openems.edge.battery.bmw.enums;

import io.openems.common.types.OptionsEnum;

public enum BatteryState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	OFF(0, "Off"), //
	INIT(2, "Init"), //
	STANDBY(4, "Standby"), //
	READY(8, "Ready"), //
	OPERATION(16, "Operation"), //
	ERROR(32, "Error"), //
	PRE_CHARGE(256, "Precharge") //
	;

	private final int value;
	private final String name;

	private BatteryState(int value, String name) {
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