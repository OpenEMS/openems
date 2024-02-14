package io.openems.edge.deye.batteryinverter.enums;

import io.openems.common.types.OptionsEnum;

public enum PhaseAngleAbrupt implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	DISABLED(0, "Disabled"), //
	ANGLE_ABRUPT_LIMIT_12_DEGREE(1, "Angle abrupt limit 12 deg"), //
	ANGLE_ABRUPT_LIMIT_6_DEGREE(2, "Angle abrupt limit 6 deg"); //

	private final int value;
	private final String name;

	private PhaseAngleAbrupt(int value, String name) {
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