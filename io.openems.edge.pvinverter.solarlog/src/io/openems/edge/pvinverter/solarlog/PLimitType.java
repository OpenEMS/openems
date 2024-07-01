package io.openems.edge.pvinverter.solarlog;

import io.openems.common.types.OptionsEnum;

public enum PLimitType implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NOT_ACTIVE(0, "Interface not active"), //
	NO_LIMIT(1, "No limitation active (100 %)"), //
	FIXED_LIMIT(2, "Fixed limitation active in %"), //
	LIMIT_SELF_SUFFICIENCY(3, "Limitation in %, considering self-sufficiency"); //

	private final int value;
	private final String name;

	private PLimitType(int value, String name) {
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