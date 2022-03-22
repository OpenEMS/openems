package io.openems.edge.sma.enums;

import io.openems.common.types.OptionsEnum;

public enum MeterSetting implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	SMA_ENERGY_METER(3053, "SMA Energy Meter"), //
	WECHSELRICHTER(3547, "Wechselrichter");

	private final int value;
	private final String name;

	private MeterSetting(int value, String name) {
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