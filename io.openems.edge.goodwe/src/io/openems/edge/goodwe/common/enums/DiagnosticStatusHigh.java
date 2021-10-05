package io.openems.edge.goodwe.common.enums;

import io.openems.common.types.OptionsEnum;

public enum DiagnosticStatusHigh implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	BATTERY_PRECHARGE_RELAY_OFF(0x00000001, "Battery Precharge Relay Off"), //
	BYPASS_RELAY_STICK(0x00000002, "ByPass Relay Stick"), //
	EXTERNAL_STOP_MODE_ENABLE(0x20000000, "External Stop Mode Enable"), //
	BATTERY_OFFGRID_DOD(0x40000000, "Battery Offgrid DOD"), //
	BATTERY_SOC_ADJUST_ENABLE(0x80000000, "Battery SOC Adjust Enable");

	private final int value;
	private final String option;

	private DiagnosticStatusHigh(int value, String option) {
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