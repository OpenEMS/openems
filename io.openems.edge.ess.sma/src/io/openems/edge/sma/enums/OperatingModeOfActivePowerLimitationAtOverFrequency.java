package io.openems.edge.sma.enums;

import io.openems.common.types.OptionsEnum;

public enum OperatingModeOfActivePowerLimitationAtOverFrequency implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	OFF(303, "Off"), //
	LINEAR_GRADIENT_FOR_INSTANTANEOUS_POWER(1132, "Linear Gradient for Instantaneous Power");

	private final int value;
	private final String name;

	private OperatingModeOfActivePowerLimitationAtOverFrequency(int value, String name) {
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