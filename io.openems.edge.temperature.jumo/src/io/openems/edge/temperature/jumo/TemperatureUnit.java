package io.openems.edge.temperature.jumo;

import io.openems.common.types.OptionsEnum;

public enum TemperatureUnit implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	DEGREE_CELSIUS(0, "Degree Celsius"), //
	DEGREE_FAHRENHEIT(1, "Degree Fahrenheit"), //
	;

	private final int value;
	private final String name;

	private TemperatureUnit(int value, String name) {
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