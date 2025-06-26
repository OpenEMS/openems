package io.openems.edge.goodwe.common.enums;

import io.openems.common.types.OptionsEnum;

public enum CpldWarningCode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	PV1_OVER_CURRENT_HW(1, "PV 2 over current hw"), //
	PV2_OVER_CURRENT_HW(2, "PV 1 over current hw"), //
	BATTERY_OVER_CURRENT_HW(3, "Battery over current hw"), //
	BUS_OVER_VOLTAGE_HW(4, "Bus over voltage hw"), //
	R_INV_OVER_CURRENT_HW(5, "R inverter over current hw"), //
	S_INV_OVER_CURRENT_HW(6, "S inverter over current hw"), //
	T_INV_OVER_CURRENT_HW(7, "T inverter over current hw"), //
	BAT_RELAY_FAIL(8, "Battery relay fail");

	private final int value;
	private final String option;

	private CpldWarningCode(int value, String option) {
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