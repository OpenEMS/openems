package io.openems.edge.battery.soltaro.single.versionb.enums;

import io.openems.common.types.OptionsEnum;

public enum AutoSetFunction implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	INIT_MODE(0x0, "Init mode"), //
	START_AUTO_SETTING(0x1, "Start auto setting"), //
	SUCCESS(0x2, "Success"), //
	FAILURE(0x3, "Failure");

	private final int value;
	private final String name;

	private AutoSetFunction(int value, String name) {
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