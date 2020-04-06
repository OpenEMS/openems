package io.openems.edge.battery.soltaro.single.versionc;

import io.openems.common.types.OptionsEnum;

public enum PreChargeControl implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	SWITCH_OFF(0x0, "Switch ON Pre-charge & Main Power Contactor"), //
	SWITCH_ON(0x1, "Shut Down Main Power Contactor & Pre-charge");

	private final int value;
	private final String name;

	private PreChargeControl(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override
	public int getValue() {
		return value;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}
