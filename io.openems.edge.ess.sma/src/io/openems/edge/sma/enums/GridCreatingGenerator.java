package io.openems.edge.sma.enums;

import io.openems.common.types.OptionsEnum;

public enum GridCreatingGenerator implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NONE(1799, "None"), //
	UTILITY_GRID(1801, "Utility-Grid"), //
	UTILITY_GRID_AND_GENERATOR(1802, "Utility Grid and Generator"), //
	INVALID_CONFIGURATION_FOR_THE_PV_PRODUCTION_METER(1803, "Invalid Configuration for the PV Production Meter");

	private final int value;
	private final String name;

	private GridCreatingGenerator(int value, String name) {
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