package io.openems.edge.ruhfass.battery.batcon.enums;

import io.openems.common.types.OptionsEnum;

public enum OperationModeBattery implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	HV_INACTVE(0, "HV Inactive"), // HV_inaktiv
	HV_ACTVE(1, "HV Active"), // HV_active
	BALANCING(2, "Balancing"), // Balancing
	EXTERNAL_CHARGING(3, "External Charging"), // externes Laden
	AC_CHARGING(4, "AC Charging"), // AC_Laden
	BATTERY_FAILURE(5, "Battery Failure"), // Batteriefehler
	DC_CHARGING(6, "DC Charging"), // DC_Laden
	INIT(7, "Init"), // Init
	; //

	private int value;
	private String name;

	private OperationModeBattery(int value, String name) {
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
