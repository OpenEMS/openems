package io.openems.edge.battery.fenecon.f2b.bmw.enums;

import io.openems.common.types.OptionsEnum;

public enum HeatingUsage implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	OFF(0, "Off"), //
	ON(1, "On"),//
	;//

	private final int value;
	private final String name;

	private HeatingUsage(int value, String name) {
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