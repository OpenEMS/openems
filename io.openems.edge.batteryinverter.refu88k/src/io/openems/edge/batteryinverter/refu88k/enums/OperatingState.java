package io.openems.edge.batteryinverter.refu88k.enums;

import io.openems.common.types.OptionsEnum;

public enum OperatingState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	OFF(1, "Off"), //
	SLEEPING(2, "Sleeping"), //
	STARTING(3, "Starting"), //
	MPPT(4, "MPPT"), //
	THROTTLED(5, "Throttled"), //
	SHUTTING_DOWN(6, "Shutting Down"), //
	FAULT(7, "Fault"), //
	STANDBY(8, "Standby"), //
	STARTED(9, "Started");

	private final int value;
	private final String name;

	private OperatingState(int value, String name) {
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
