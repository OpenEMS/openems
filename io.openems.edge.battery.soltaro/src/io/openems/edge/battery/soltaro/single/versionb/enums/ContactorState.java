package io.openems.edge.battery.soltaro.single.versionb.enums;

import io.openems.common.types.OptionsEnum;

public enum ContactorState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	START(0x1, "Start"), //
	STOP(0x2, "Stop");

	private final int value;
	private final String name;

	private ContactorState(int value, String name) {
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