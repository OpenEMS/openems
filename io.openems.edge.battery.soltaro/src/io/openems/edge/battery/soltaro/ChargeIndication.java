package io.openems.edge.battery.soltaro;

import io.openems.edge.common.channel.doc.OptionsEnum;

public enum ChargeIndication implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	STANDING(0, "Standing"), //
	DISCHARGING(1, "Discharging"), //
	CHARGING(2, "Charging");

	private int value;
	private String name;

	private ChargeIndication(int value, String name) {
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