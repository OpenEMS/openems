package io.openems.edge.batteryinverter.refu88k.enums;

import io.openems.common.types.OptionsEnum;

public enum PcsSetOperation implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NO_COMMAND(0, "No Command"), //
	START_PCS(1, "Connect to grid"), //
	STOP_PCS(2, "Stop system"), //
	ENTER_STANDBY_MODE(3, "Enter Standby Mode"), //
	EXIT_STANDBY_MODE(4, "Exit Standby Mode"),;

	private final int value;
	private final String name;

	private PcsSetOperation(int value, String name) {
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
