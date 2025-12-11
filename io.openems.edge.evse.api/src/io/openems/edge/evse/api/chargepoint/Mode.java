package io.openems.edge.evse.api.chargepoint;

import io.openems.common.types.OptionsEnum;

public enum Mode implements OptionsEnum {
	ZERO(0, "Zero"), //
	MINIMUM(1, "Minimum charge"), //
	SURPLUS(2, "Surplus charge"), //
	FORCE(3, "Force charge") //
	;

	private final int value;
	private final String name;

	private Mode(int value, String name) {
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
		return ZERO;
	}
}
