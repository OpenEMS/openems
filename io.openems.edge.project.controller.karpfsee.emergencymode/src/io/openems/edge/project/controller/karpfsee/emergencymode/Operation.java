package io.openems.edge.project.controller.karpfsee.emergencymode;

import io.openems.common.types.OptionsEnum;

enum Operation implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	SET_ZERO(0, "Set Zero"), //
	SET_ONE(1, "Set One");

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