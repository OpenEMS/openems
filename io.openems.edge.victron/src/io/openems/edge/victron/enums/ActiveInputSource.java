package io.openems.edge.victron.enums;

import io.openems.common.types.OptionsEnum;

public enum ActiveInputSource implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	UNKNOWN(0, "Unknown"), //
	GRID(1, "Grid"), //
	GENERATOR(2, "Generator"), //
	SHORE_POWER(3, "Shore Power"), //
	NOT_CONNECTED(240, "Not connected");

	private final int value;
	private final String name;

	private ActiveInputSource(int value, String name) {
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
