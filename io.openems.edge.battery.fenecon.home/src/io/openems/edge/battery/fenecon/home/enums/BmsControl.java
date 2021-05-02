package io.openems.edge.battery.fenecon.home.enums;

import io.openems.common.types.OptionsEnum;

public enum BmsControl implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	SWITCHED_ON(0, "Switch ON Pre-charge & Main Power Contactor"), //
	SWITCHED_OFF(0x1, "Shut Down Main Power Contactor & Pre-charge"), //
	// 0x2 is not documented in the modbus protocol, but we still read it from time
	// to time.
	IGNORED(0x2, "Switch ON Pre-charge & Main Power Contactor");

	private final int value;
	private final String name;

	private BmsControl(int value, String name) {
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
