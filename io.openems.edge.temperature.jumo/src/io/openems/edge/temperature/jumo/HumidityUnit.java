package io.openems.edge.temperature.jumo;

import io.openems.common.types.OptionsEnum;

public enum HumidityUnit implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	GRAM_PER_CUBIC_METER(0, "Gram per cubic meter g/m³"), //
	GRAMS_PER_CUBIC_FOOT(1, "Gram per cubic foot g/ft³"), //
	;

	private final int value;
	private final String name;

	private HumidityUnit(int value, String name) {
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
