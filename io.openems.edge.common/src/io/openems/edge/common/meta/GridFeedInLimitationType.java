package io.openems.edge.common.meta;

import io.openems.common.types.OptionsEnum;

public enum GridFeedInLimitationType implements OptionsEnum {
	UNDEFINED(-1, "UNDEFINED"), //
	NO_LIMITATION(0, "No limitation"), //
	DYNAMIC_LIMITATION(1, "Dynamic limitation"); //

	private final int value;
	private final String name;

	GridFeedInLimitationType(int value, String name) {
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
