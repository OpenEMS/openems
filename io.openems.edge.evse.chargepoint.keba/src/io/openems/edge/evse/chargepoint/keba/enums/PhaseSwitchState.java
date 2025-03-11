package io.openems.edge.evse.chargepoint.keba.enums;

import io.openems.common.types.OptionsEnum;

public enum PhaseSwitchState implements OptionsEnum {
	UNDEFINED(-1, "Undefined", null), //
	SINGLE(0, "1 phase", Actual.SINGLE), //
	THREE(1, "3 phases", Actual.THREE);

	public enum Actual {
		SINGLE, THREE;
	}

	public final Actual actual;

	private final int value;
	private final String name;

	private PhaseSwitchState(int value, String name, Actual actual) {
		this.value = value;
		this.name = name;
		this.actual = actual;
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