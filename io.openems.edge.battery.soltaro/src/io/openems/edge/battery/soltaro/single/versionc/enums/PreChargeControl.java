package io.openems.edge.battery.soltaro.single.versionc.enums;

import io.openems.common.types.OptionsEnum;

public enum PreChargeControl implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	SWITCH_OFF(0x0, "Shut Down Main Power Contactor & Pre-charge"), //
	SWITCH_ON(0x1, "Switch ON Pre-charge & Main Power Contactor"), //
	PRE_CHARGING(0x2, "Intermediate State: Pre-charging"), //
	RUNNING(0x3, "Running");

	private final int value;
	private final String name;

	private PreChargeControl(int value, String name) {
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
