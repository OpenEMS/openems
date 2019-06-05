package io.openems.edge.ess.refu;

import io.openems.common.types.OptionsEnum;

public enum BatteryMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NORMAL(0, "Normal Mode");

	private final int value;
	private final String option;

	private BatteryMode(int value, String option) {
		this.value = value;
		this.option = option;
	}

	@Override
	public int getValue() {
		return value;
	}

	@Override
	public String getName() {
		return option;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}