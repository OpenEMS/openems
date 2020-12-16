package io.openems.edge.ess.mr.gridcon.enums;

import io.openems.common.types.OptionsEnum;

public enum PControlMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	DISABLED(1, "Disabled"), //
	ACTIVE_POWER_CONTROL(1, "Active Power Control Mode"), //
	POWER_LIMITER(2, "Power Limiter Mode"); //

	private final int value;
	private final String name;

	private PControlMode(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}
