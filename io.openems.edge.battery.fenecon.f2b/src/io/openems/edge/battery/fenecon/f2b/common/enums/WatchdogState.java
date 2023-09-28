package io.openems.edge.battery.fenecon.f2b.common.enums;

import io.openems.common.types.OptionsEnum;

public enum WatchdogState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	DISABLED(0, "Disabled"), //
	OK(1, "Ok"), //
	TRIGGERED(2, "Triggered"), //
	;//

	private final int value;
	private final String name;

	private WatchdogState(int value, String name) {
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