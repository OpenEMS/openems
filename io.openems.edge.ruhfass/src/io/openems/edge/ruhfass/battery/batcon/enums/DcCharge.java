package io.openems.edge.ruhfass.battery.batcon.enums;

import io.openems.common.types.OptionsEnum;

public enum DcCharge implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NO_DC_CHARGE(0, "No DC Charge"), //
	DC_CHARGE(1, "DC Charge"); //

	private int value;
	private String name;

	private DcCharge(int value, String name) {
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
