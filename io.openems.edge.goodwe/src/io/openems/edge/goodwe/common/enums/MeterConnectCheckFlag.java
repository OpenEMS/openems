package io.openems.edge.goodwe.common.enums;

import io.openems.common.types.OptionsEnum;

public enum MeterConnectCheckFlag implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	STOP(0, "Stop Checking"), //
	CHECKING(1, "Checking"), //
	WAIT(2, "Wait for check"); //

	private final int value;
	private final String option;

	private MeterConnectCheckFlag(int value, String option) {
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