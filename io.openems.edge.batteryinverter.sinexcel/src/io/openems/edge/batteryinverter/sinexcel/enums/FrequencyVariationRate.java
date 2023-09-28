package io.openems.edge.batteryinverter.sinexcel.enums;

import io.openems.common.types.OptionsEnum;

public enum FrequencyVariationRate implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	DISABLED(0, "Disabled"), //
	RATE_0125(1, "Rate limit 0.125 Hz/s"), //
	RATE_02(2, "Rate limit 0.2 Hz/s");//

	private final int value;
	private final String name;

	private FrequencyVariationRate(int value, String name) {
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