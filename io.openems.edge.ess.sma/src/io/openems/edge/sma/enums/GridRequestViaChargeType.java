package io.openems.edge.sma.enums;

import io.openems.common.types.OptionsEnum;

public enum GridRequestViaChargeType implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	OFF(303, "Off"), //
	FULL_AND_EQUALIZATION_CHARGE(1736, "Full and Equalization Charge"), //
	FULL_CHARGE(1768, "Full Charge"), //
	EQUALIZATION_CHARGE(1769, "Equalization Charge");

	private final int value;
	private final String name;

	private GridRequestViaChargeType(int value, String name) {
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