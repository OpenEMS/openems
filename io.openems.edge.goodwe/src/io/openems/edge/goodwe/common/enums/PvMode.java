package io.openems.edge.goodwe.common.enums;

import io.openems.common.types.OptionsEnum;

public enum PvMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NO_PV(0, "Inverter disconnects to PV"), //
	STANDBY(1, "PV does not output power"), //
	WORK(2, "PV output power");

	private final int value;
	private final String option;

	private PvMode(int value, String option) {
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