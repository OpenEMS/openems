package io.openems.edge.deye.enums;

import io.openems.common.types.OptionsEnum;
// register 500
public enum InverterRunState implements OptionsEnum {
    UNDEFINED(-1, "Undefined"),
    STANDBY(0, "Standby"),
    SELF_CHECK(1, "Self check"),
    NORAML(2, "Normal"),
    FAULT(3, "Fault");

	private final int value;
	private final String name;

	private InverterRunState(int value, String name) {
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
