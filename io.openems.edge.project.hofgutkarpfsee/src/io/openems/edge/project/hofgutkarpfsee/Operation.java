package io.openems.edge.project.hofgutkarpfsee;

import io.openems.common.types.OptionsEnum;

enum Operation implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	RUN(0, "RUN"), //
	STOP(1, "STOP"),//
	ON(2, "ON"),//
	OFF(3, "OFF");

	private final int value;
	private final String name;

	private Operation(int value, String name) {
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