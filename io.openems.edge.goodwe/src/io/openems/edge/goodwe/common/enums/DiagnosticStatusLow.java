package io.openems.edge.goodwe.common.enums;

import io.openems.common.types.OptionsEnum;

public enum DiagnosticStatusLow implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	BATTERY_VOLT_LOW(0, "Battery not dischargecuased by low voltage"), //
	BATTERY_SOC_LOW(1, "Battery not discharge caused by low SOC "), //
	BATTERY_SOC_IN_BACK(2, "Battery SOC not recover to allow-discharge level"), //
	BMS_DISCHARGE_DISABLE(3, "BMS not allow discharge "), //
	DISCHARGE_TIME_ON(4, "Discharge time is set, 1: On, 0: OFF "), //
	CHARGE_TIME_ON(5, "Charge time is set, 1: On, 0: OFF"), //
	DISCHARGE_DRIVE_ON(6, "Discharge driver is turned on "), //
	BMS_DISCHARGE_CURRENT_LOW(7, "BMS discharge current limit is too low "), //
	DISCHARGE_CURRENT_LOW(8, "Discharge current limit is too low (from App) "), //
	METER_COMM_LOSS(9, "Smart Meter communication failure "), //
	METER_CONNECT_REVERSE(10, "Smart Meter connection reversed "), //
	SELF_USE_LOAD_LIGHT(11, "Low load power, cannot activate battery discharge "), //
	EMS_DISCHARGE_CURRENT_ZERO(12, "Discharge current limit 0A from EMS "), //
	DISCHARGE_BUS_HIGH(13, "Battery not discharge caused by over high PV voltage "), //
	BATTERY_DISCONNECT(14, "Battery disconnected "), //
	BATTERY_OVERCHARGE(15, "Battery overcharged "), //
	BMS_OVER_TEMPERATURE(16, "Lithium battery over temperature "), //
	BMS_OVER_CHARGE(17, "Lithium battery overcharged or an individual cell voltage is higher"), //
	BMS_CHARGE_DISABLE(18, "BMS does not allow charge "), //
	SELF_USE_OFF(19, "Self-use mode turned off "), //
	SOC_DELTA_OVER_RANGE(20, "SOC Jumps abnormally "), //
	BATTERY_SELF_DISCHARGE(21, "Battery discharge at low current for long time, continuously over 30% of battery SOC "), //
	OFF_GRID_SOC_LOW(22, "SOC is low under off-grid statues"), //
	GRID_WAVE_UNSTABLE(23, "Grid wave is bad, switch to back-up mode frequently "), //
	FEED_POWER_LIMIT(24, "Export power limit is set "), //
	PF_VALUE_SET(25, "PF value is set "), //
	REAL_POWER_LIMIT(26, "Active power value is set "), //
	SOC_PROTECT_OFF(28, "SOC protect Off ");

	private final int value;
	private final String option;

	private DiagnosticStatusLow(int value, String option) {
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