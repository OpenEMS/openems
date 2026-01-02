package io.openems.edge.victron.enums;

import io.openems.common.types.OptionsEnum;

public enum Error implements OptionsEnum {
	UNDEFINED(-1, "undefined"), //
	NO_ERROR(0, "No error"), //
	BATTERY_INITIALIZATION_ERROR(1, "Battery initialization error"), //
	NO_BATTERIES_CONNECTED(2, "No batteries connected"), //
	UNKNOWN_BATTERY_CONNECTED(3, "Unknown battery connected"), //
	DIFFERENT_BATTERY_TYPE(4, "Different battery type"), //
	NUMBER_OF_BATTERIES_INCORRECT(5, "Number of batteries incorrect"), //
	LYNX_SHUNT_NOT_FOUND(6, "Lynx shunt not found"), //
	BATTERY_MEASURE_ERROR(7, "Battery measure error"), //
	INTERNAL_CALCULATION_ERROR(8, "Internal calculation error"), //
	BATTERIES_IN_SERIES_NOT_OK(9, "Batteries in series not ok"), //
	NUMBER_OF_BATTERIES_INCORRECT_2(10, "Number of batteries incorrect"), //
	HARDWARE_ERROR(11, "Hardware error"), //
	WATCHDOG_ERROR(12, "Watchdog error"), //
	OVER_VOLTAGE(13, "Over voltage"), //
	UNDER_VOLTAGE(14, "Under voltage"), //
	OVER_TEMPERATURE(15, "Over temperature"), //
	UNDER_TEMPERATURE(16, "Under temperature"), //
	HARDWARE_FAULT(17, "Hardware fault"), //
	STANDBY_SHUTDOWN(18, "Standby shutdown"), //
	PRE_CHARGE_CHARGE_ERROR(19, "Pre-charge charge error"), //
	SAFETY_CONTACTOR_CHECK_ERROR(20, "Safety contactor check error"), //
	PRE_CHARGE_DISCHARGE_ERROR(21, "Pre-charge discharge error"), //
	ADC_ERROR(22, "ADC error"), //
	SLAVE_ERROR(23, "Slave error"), //
	SLAVE_WARNING(24, "Slave warning"), //
	PRE_CHARGE_ERROR(25, "Pre-charge error"), //
	SAFETY_CONTACTOR_ERROR(26, "Safety contactor error"), //
	OVER_CURRENT(27, "Over current"), //
	SLAVE_UPDATE_FAILED(28, "Slave update failed"), //
	SLAVE_UPDATE_UNAVAILABLE(29, "Slave update unavailable"), //
	CALIBRATION_DATA_LOST(30, "Calibration data lost"), //
	SETTINGS_INVALID(31, "Settings invalid"), //
	BMS_CABLE(32, "BMS cable"), //
	REFERENCE_FAILURE(33, "Reference failure"), //
	WRONG_SYSTEM_VOLTAGE(34, "Wrong system voltage"), //
	PRE_CHARGE_TIMEOUT(35, "Pre-charge timeout");

	private final int value;
	private final String option;

	private Error(int value, String option) {
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
