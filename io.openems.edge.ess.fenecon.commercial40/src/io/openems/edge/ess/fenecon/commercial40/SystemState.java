package io.openems.edge.ess.fenecon.commercial40;

import io.openems.common.types.OptionsEnum;

public enum SystemState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	STARTING(2, "Stop"), //
	PV_CHARGE(4, "PV-Charge"), //
	STANDBY(8, "Standby"), //
	START(16, "Start"), //
	FAULT(32, "Fault"), //
	DEBUG(64, "Debug");

	private final int value;
	private final String name;

	private SystemState(int value, String name) {
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