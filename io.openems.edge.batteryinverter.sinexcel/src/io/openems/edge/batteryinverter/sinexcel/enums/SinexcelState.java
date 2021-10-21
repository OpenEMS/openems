package io.openems.edge.batteryinverter.sinexcel.enums;

import io.openems.common.types.OptionsEnum;

public enum SinexcelState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	OFF(1, "Off"), //
	SLEEPING(2, "Sleeping"), //
	STARTING(3, "Starting"), //
	MPPT(4, "MPPT"), //
	THROTTLED(5, "Throttled"), //
	SHUTTINGDOWN(6, "Shutting Down"), //
	FAULT(7, "Fault"), //
	STANDBY(8, "Standby"), //
	STARTED(9, "Started");

	private final int value;
	private final String option;

	private SinexcelState(int value, String option) {
		this.value = value;
		this.option = option;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.option;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}

}
