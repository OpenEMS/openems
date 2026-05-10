package io.openems.edge.victron.enums;

import io.openems.common.types.OptionsEnum;

public enum MppOperationMode implements OptionsEnum {

	UNDEFINED(-1, "undefined"), //
	OFF(0, "Off"), //
	VOLTAGE_OR_CURRENT_LIMITED(1, "Voltage/Current limited"), //
	MPPT_ACTIVE(2, "MPPT active"), //
	NOT_AVAILABLE(255, "not available");

	private final int value;
	private final String option;

	private MppOperationMode(int value, String option) {
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
