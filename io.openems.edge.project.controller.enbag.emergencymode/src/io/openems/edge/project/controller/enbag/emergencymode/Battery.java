package io.openems.edge.project.controller.enbag.emergencymode;

import io.openems.common.types.OptionsEnum;

enum Battery implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	ESS1(0, "Ess1"), //
	ESS2(1, "Ess2");

	private final int value;
	private final String name;

	private Battery(int value, String name) {
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