package io.openems.edge.ess.sinexcel.enums;

import io.openems.common.types.OptionsEnum;

/**
 * This enum holds the common Sinexcel definition for 0 = false and 1 = true.
 */
public enum FalseTrue implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	FALSE(0, "False"), //
	TRUE(1, "True"); //

	private final int value;
	private final String name;

	private FalseTrue(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override
	public int getValue() {
		return value;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}