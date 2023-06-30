package io.openems.edge.battery.soltaro.cluster.enums;

import io.openems.common.types.OptionsEnum;

public enum RunningState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NORMAL(0, "Normal"), // Allow discharge and charge
	FULLY_CHARGED(1, "Fully charged"), // Allow discharge, deny charge
	EMPTY(2, "Empty"), // Allow charge, deny discharge
	STANDBY(3, "Standby"), // deny discharge, deny charge
	STOPPED(4, "Stopped"); // deny discharge, deny charge

	private final int value;
	private final String name;

	private RunningState(int value, String name) {
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