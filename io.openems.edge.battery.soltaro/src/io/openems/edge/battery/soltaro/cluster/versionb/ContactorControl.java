package io.openems.edge.battery.soltaro.cluster.versionb;

import io.openems.common.types.OptionsEnum;

public enum ContactorControl implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	CUT_OFF(0, "Cut off"), //
	CONNECTION_INITIATING(1, "Connection initiating"), //
	ON_GRID(3, "On grid");

	private final int value;
	private final String name;

	private ContactorControl(int value, String name) {
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