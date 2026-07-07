package io.openems.edge.victron.enums;

import io.openems.common.types.OptionsEnum;

public enum SwitchPosition implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	CHARGER_ONLY(1, "Charger Only"), //
	INVERTER_ONLY(2, "Inverter Only"), //
	ON(3, "On"), //
	OFF(4, "Off");

	private final int value;
	private final String option;

	private SwitchPosition(int value, String option) {
		this.value = value;
		this.option = option;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.option;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}
