package io.openems.edge.fenecon.mini.ess;

import io.openems.common.types.OptionsEnum;

public enum DebugRunState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	FORBID(0, "Forbid"), //
	CHARGE(1, "Charge"), //
	DISCHARGE(2, "Discharge"); //

	private final int value;
	private final String name;

	private DebugRunState(int value, String name) {
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