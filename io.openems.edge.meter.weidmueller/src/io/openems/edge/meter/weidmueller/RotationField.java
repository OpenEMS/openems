package io.openems.edge.meter.weidmueller;

import io.openems.common.types.OptionsEnum;

public enum RotationField implements OptionsEnum {
	UNDEFINED(-2, "Undefined"), //
	RIGHT(1, "right"), //
	NONE(0, "none"), //
	LEFT(-1, "left"); //

	private final int value;
	private final String name;

	private RotationField(int value, String name) {
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