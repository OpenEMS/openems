package io.openems.edge.battery.fenecon.f2b.bmw.enums;

import io.openems.common.types.OptionsEnum;

public enum RequestCharging implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NO_CHARGING(0, "No Charging"), //
	NORMAL_CHARGING(1, "Normal Charging"), //
	CHARGING_WITH_PRECONDITIONING(2, "Charging with preconditioning"), //
	;//

	private final int value;
	private final String name;

	private RequestCharging(int value, String name) {
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