package io.openems.edge.fronius.enums;

import io.openems.common.types.OptionsEnum;

public enum PowerSupplyStatus implements OptionsEnum {
	UNDEFINED(-1, "Undefiniert"),
	GRID_CONNECTED(0, "Netzbetrieb"),
	EMERGENCY_POWER(1, "Notstrombetrieb / Backup");

	private final int value;
	private final String name;

	private PowerSupplyStatus(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override
	public int getValue() { return this.value; }

	@Override
	public String getName() { return this.name; }

	@Override
	public OptionsEnum getUndefined() { return UNDEFINED; }
}