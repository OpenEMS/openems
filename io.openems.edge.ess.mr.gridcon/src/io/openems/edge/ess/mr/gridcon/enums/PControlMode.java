package io.openems.edge.ess.mr.gridcon.enums;

import io.openems.common.types.OptionsEnum;

public enum PControlMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	DISABLED(1, "Disabled"), // TODO Check values!!!
	ACTIVE_POWER_CONTROL(1, "Active Power Control Mode"), // TODO maybe inverted word order?!
	POWER_LIMITER(4, "Power Limiter Mode");

	private final float value;
	private final String name;

	private PControlMode(float value, String name) {
		this.value = value;
		this.name = name;
	}

	public float getFloatValue() {
		return value;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public int getValue() {
		return 0;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}
