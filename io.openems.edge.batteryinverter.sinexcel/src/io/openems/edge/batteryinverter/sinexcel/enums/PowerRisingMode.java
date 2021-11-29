package io.openems.edge.batteryinverter.sinexcel.enums;

import io.openems.common.types.OptionsEnum;

public enum PowerRisingMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	STEP(0, "Step Function"), //
	RAMP(1, "Ramp Function");//

	private final int value;
	private final String name;

	private PowerRisingMode(int value, String name) {
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