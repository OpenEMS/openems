package io.openems.edge.pytes.enums;

import io.openems.common.types.OptionsEnum;

public enum WorkMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	EXTERNAL(0, "ESS controlled by OpenEMS"), //
	INTERNAL_ZERO_EXPORT(1, "Internal ctontrol mode. Self-consumption, zero export to grid"), //
	INTERNAL_GRID_FEED_IN(2, "Internal ctontrol mode. Self-consumption, export to grid allowed"), //
	STOP(4, "Stop"), //
	STANDBY(32, "Standby");

	private final int value;
	private final String name;

	private WorkMode(int value, String name) {
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