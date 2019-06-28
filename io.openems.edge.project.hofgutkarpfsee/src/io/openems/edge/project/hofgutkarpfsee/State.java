package io.openems.edge.project.hofgutkarpfsee;

import io.openems.common.types.OptionsEnum;

enum State implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	BELOW_THRESHOLD(0, "Below Threshold"), //
	PASS_THRESHOLD_COMING_FROM_ABOVE(1, "Pass Threshold Coming From Above"), //
	ABOVE_THRESHOLD(2, "Above Threshold");
	
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