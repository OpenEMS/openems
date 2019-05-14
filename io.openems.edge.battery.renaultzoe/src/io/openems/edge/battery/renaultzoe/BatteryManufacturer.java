package io.openems.edge.battery.renaultzoe;

import io.openems.common.types.OptionsEnum;

public enum BatteryManufacturer implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	RENAULT(0, "Renault"); //
	
	private int value;
	private String name;

	private BatteryManufacturer(int value, String name) {
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
