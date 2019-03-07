package io.openems.edge.project.controller.enbag.emergencymode;

import io.openems.edge.common.channel.doc.OptionsEnum;

public enum BatteryState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //

	BATTERY_LOW(0, "Battery Soc Less Than 5 Percent"), //

	BATTERY_HIGH(1, "Battery Soc More Than 95 Percent"), //

	BATTERY_OKAY(2, "Battery SOC Between 5 and 95 Percent ");

	private final int value;
	private final String name;

	private BatteryState(int value, String name) {
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