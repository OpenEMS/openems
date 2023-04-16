package io.openems.edge.evcs.webasto.next.enums;

import io.openems.common.types.OptionsEnum;

public enum EvseErrorCode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NO_ERROR(0, "No error"), //
	POWER_SWITCH_FAILURE(1, "Error internal board supply voltage, 5V Ref Voltage"), //
	SUPPLY_VOLTAGE_ERROR(2, "Supply voltage error"), //
	EV_COMMUNICATION_ERROR(3, "EV communication error"), //
	OVER_VOLTAGE(4, "Over Voltage"), //
	UNDER_VOLTAGE(5, "Under Voltage"), //
	OVER_CURRENT_FALIURE(6, "Over current faliure"), //
	OTHER_ERROR(7, "Other error"), //
	GROUND_FAILURE(8, "Ground failure"), //
	RCD_MODULE_ERROR(9, "Error RCD modulel"), //
	HIGH_TEMPERATURE(10, "Error overtemperature"), //
	PROXIMIOTY_PILOT_ERROR(11, "Proximity Pilot Error"), //
	SHUTTER_ERROR(12, "Shutter Error"), //
	THREE_PHASE_CHECK_ERROR(13, "Three phase check error"), //
	PWR_INTERNAL_ERROR(14, "PWR internal error"), //
	NEGATIVE_CONTROL_PILOT_OUT_OF_RANGE(15, "negative control pilot out of range"), //
	// RELAY_WELDED_OPEN_ERRPR(16,"relay welded open"), //
	DC_RESIDUAL_CURRENT(16, "DC residual current")//
	;

	private final int value;
	private final String name;

	private EvseErrorCode(int value, String name) {
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
