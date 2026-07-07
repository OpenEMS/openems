package io.openems.edge.victron.enums;

import io.openems.common.types.OptionsEnum;

public enum ErrorCode implements OptionsEnum {

	UNDEFINED(-1, "undefined"), //
	NO_ERROR(0, "No error"), //
	BATTERY_TEMPERATURE_TOO_HIGH(1, "Battery temperature too high"), //
	BATTERY_VOLTAGE_TOO_HIGH(2, "Battery voltage too high"), //
	BATTERY_TEMPERATURE_SENSOR_MISWIRED_PLUS(3, "Battery temperature sensor miswired (+)"), //
	BATTERY_TEMPERATURE_SENSOR_MISWIRED_MINUS(4, "Battery temperature sensor miswired (-)"), //
	BATTERY_TEMPERATURE_SENSOR_DISCONNECTED(5, "Battery temperature sensor disconnected"), //
	BATTERY_VOLTAGE_SENSE_MISWIRED_PLUS(6, "Battery voltage sensor miswired (+)"), //
	BATTERY_VOLTAGE_SENSE_MISWIRED_MINUS(7, "Battery voltage sensor miswired (-)"), //
	BATTERY_VOLTAGE_SENSE_DISCONNECTED(8, "Battery voltage sensor disconnected"), //
	BATTERY_VOLTAGE_WIRE_LOSSES_TOO_HIGH(9, "Battery voltage wire losses too high"), //
	CHARGER_TEMPERATURE_TO_HIGH(17, "Charger temperature too high"), //
	CHARGER_OVER_CURRENT(18, "Charger over-current"),
	CHARGER_CURRENT_POLARITY_REVERSED(19, "Charger current polarity reversed"),
	BULK_TIME_LIMIT_REACHED(20, "Bulk time limit reached"), //
	CHARGER_TEMPERATURE_SENSOR_MISWIRED(22, "Charger temperature sensor miswired"), //
	CHARGER_TEMPERATURE_SENSOR_DISCONNECTED(23, "Charger temperature sensor disconnected"), //
	INPUT_CURRENT_TOO_HIGH(34, "Input current too high");

	private final int value;
	private final String option;

	private ErrorCode(int value, String option) {
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
