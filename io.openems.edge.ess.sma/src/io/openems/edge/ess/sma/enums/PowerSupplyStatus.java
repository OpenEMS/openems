package io.openems.edge.ess.sma.enums;

import io.openems.common.types.OptionsEnum;

public enum PowerSupplyStatus implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	OFF(303, "Off"), //
	UTILITY_GRID_CONNECTED(1461, "Utility Grid Connected"), //
	BACKUP_NOT_AVAILABLE(1462, "Backup Not Available"), //
	BACKUP(1463, "Backup"); //

	private final int value;
	private final String name;

	private PowerSupplyStatus(int value, String name) {
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