package io.openems.edge.battery.bydcommercial.enums;

import io.openems.common.types.OptionsEnum;

public enum PowerCircuitControl implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	SWITCH_OFF(0x0, "Shut Down Main Power Contactor & Pre-charge"), //
	PRE_CHARGING_1(0x1, "Intermediate State: Pre-charging"), //
	PRE_CHARGING_2(0x2, "Intermediate State: Pre-charging"), //
	SWITCH_ON(0x3, "Switch On Main Power Contactor, Running Mode"), //
	PRE_CHARGE_FAIL(0x4, "Switch ON Pre-charge & Main Power Contactor");

	private final int value;
	private final String name;

	private PowerCircuitControl(int value, String name) {
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
