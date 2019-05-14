package io.openems.edge.battery.renaultzoe;

import io.openems.common.types.OptionsEnum;

public enum StartStopBatteryStack implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	STOP(0, "Stop"), //
	START(1, "Start"); //

	
	private int value;
	private String name;

	private StartStopBatteryStack(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override
	public int getValue() {
		return value;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}
