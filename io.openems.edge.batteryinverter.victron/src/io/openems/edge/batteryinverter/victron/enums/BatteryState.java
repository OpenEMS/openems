package io.openems.edge.batteryinverter.victron.enums;

import io.openems.common.types.OptionsEnum;

public enum BatteryState implements OptionsEnum {
	UNDEFINED(-1, "undefined"),
	IDLE(0, "idle"),
	CHARGING(1, "charging"),
	DISCHARGING(2, "discharging");
	
	
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
