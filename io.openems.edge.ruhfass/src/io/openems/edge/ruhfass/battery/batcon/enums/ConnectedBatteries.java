package io.openems.edge.ruhfass.battery.batcon.enums;

import io.openems.common.types.OptionsEnum;

public enum ConnectedBatteries implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	ONE_BATTERY_CONNECTED(0, "One Battery conncted - 400V"), //
	TWO_BATTERIES_CONNECTED(1, "Two Batteries connected - 800V"); //

	private int value;
	private String name;

	private ConnectedBatteries(int value, String name) {
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
