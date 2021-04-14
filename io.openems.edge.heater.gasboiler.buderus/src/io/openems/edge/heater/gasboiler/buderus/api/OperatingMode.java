package io.openems.edge.heater.gasboiler.buderus.api;

import io.openems.common.types.OptionsEnum;

public enum OperatingMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	SET_POINT_TEMPERATURE(0, "Set point temperature"), //
	SET_POINT_POWER_PERCENT(1, "Set point power percent"); //

	private int value;
	private String name;

	private OperatingMode(int value, String name) {
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