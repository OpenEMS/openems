package io.openems.edge.project.controller.karpfsee.emergencymode;

import io.openems.common.types.OptionsEnum;

enum State implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	PASS_HIGH_COMING_FROM_BELOW(0, "Pass High Coming From Below"), //
	PASS_HIGH_COMING_FROM_ABOVE(1, "Pass High Coming From Above"), //
	ABOVE_HIGH(2, "Above High");
	
	private final int value;
	private final String name;

	private State(int value, String name) {
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