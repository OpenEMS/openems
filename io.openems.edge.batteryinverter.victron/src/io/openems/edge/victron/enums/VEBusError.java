package io.openems.edge.victron.enums;

import io.openems.common.types.OptionsEnum;

/**
 * VE.Bus error codes.
 *
 * <p>
 * These values represent error conditions of the Victron VE.Bus system
 * (Multiplus, Quattro, etc.) as reported via Modbus register 32.
 */
public enum VeBusError implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NO_ERROR(0, "No error"), //
	SWITCHED_OFF(1, "Device is switched off because one of the other phases in the system has switched off"), //
	MIXED_TYPES(2, "New and old types MK2 are mixed in the system"), //
	DEVICES_MISSING(3, "Not all- or more than- the expected devices were found in the system"), //
	NO_DEVICE_DETECTED(4, "No other device whatsoever detected"), //
	AC_OUT_OVERVOLTAGE(5, "Overvoltage on AC-out"), //
	DDC_PROGRAM_ERROR(6, "Error in DDC Program"), //
	BMS_NO_ASSISTANT(7, "VE.Bus BMS connected, which requires an Assistant, but no assistant found"), //
	SYSTEM_TIME_SYNC(10, "System time synchronisation problem occurred"), //
	DATA_TRANSMISSION_ERROR(14, "Device cannot transmit data"), //
	DONGLE_MISSING(16, "Dongle missing"), //
	MASTER_FAILED(17, "One of the devices assumed master status because the original master failed"), //
	AC_OUT_OVERVOLTAGE_SLAVE(18, "AC Overvoltage on the output of a slave has occurred while already switched off"), //
	SLAVE_FUNCTION_ERROR(22, "This device cannot function as slave"), //
	SWITCH_OVER_PROTECTION(24, "Switch-over system protection initiated"), //
	FIRMWARE_INCOMPATIBILITY(25,
			"Firmware incompatibility. The firmware of one of the connected device is not sufficiently up to date to operate in conjunction with this device"), //
	INTERNAL_ERROR(26, "Internal error");

	private final int value;
	private final String option;

	private VeBusError(int value, String option) {
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
