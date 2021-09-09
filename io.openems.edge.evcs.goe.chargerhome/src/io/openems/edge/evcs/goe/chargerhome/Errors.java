package io.openems.edge.evcs.goe.chargerhome;

import io.openems.common.types.OptionsEnum;

public enum Errors implements OptionsEnum {
	UNDEFINED(0, "Undefined"), //
	RCCB(1, "Residual current operated device Error"), //
	PHASE(3, "Phase Error"), //
	NO_GROUND(8, "No Ground"), //
	INTERNAL(10, "Internal error"); //

	private final int value;
	private final String name;

	private Errors(int value, String name) {
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