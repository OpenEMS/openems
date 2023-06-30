package io.openems.edge.battery.bmw.enums;

import io.openems.common.types.OptionsEnum;

public enum BmsState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	OFF(0, "Off"), //
	INIT(2, "Init"), //
	STANDBY(4, "Standby"), //
	READY(8, "Ready"), //
	OPERATION(16, "Operation"), //
	ERROR(32, "Error"), //
	PRE_HEAT(64, "Pre-Heat"), //
	PRE_HEAT_COMPLETED(128, "Pre-Heat completed"), //
	PRE_CHARGE(256, "Precharge"), //
	PRE_CHARGE_COMPLETED(512, "Precharge completed"), //
	STATE_UNKNOWN(32768, "Unknown undefined");

	private final int value;
	private final String name;

	private BmsState(int value, String name) {
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
