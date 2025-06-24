package io.openems.edge.evse.chargepoint.heidelberg.connect.enums;

import io.openems.common.types.OptionsEnum;

public enum PhaseSwitchControl implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	SINGLE(0, "1 phase"), //
	THREE(3, "3 phases");

	private final int value;
	private final String name;

	private PhaseSwitchControl(int value, String name) {
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