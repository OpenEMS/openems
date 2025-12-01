package io.openems.edge.victron.enums;

import io.openems.common.types.OptionsEnum;

public enum Alarm implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NO_ALARM(0, "No alarm"), //
	WARNING(1, "Warning"), //
	ALARM(2, "Alarm");

	private final int value;
	private final String name;

	private Alarm(int value, String name) {
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
