package io.openems.edge.controller.heatingelement;

import io.openems.common.types.OptionsEnum;

public enum ModeType implements OptionsEnum {
	PRIORITY_MODE(0, "Time or KWH?"),//
	NORMAL_MODE(1, "Normal algorithm mode");//

	private final int value;
	private final String name;

	private ModeType(int value, String name) {
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
		return NORMAL_MODE;
	}
}
