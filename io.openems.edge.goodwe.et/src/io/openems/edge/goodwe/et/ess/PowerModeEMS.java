package io.openems.edge.goodwe.et.ess;

import io.openems.common.types.OptionsEnum;

public enum PowerModeEMS implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	STOPPED(255, "Stop connection to grid and turn to wait mode"), //
	AUTO(1, "Self-Use mode, Smart Meter Com. Normal, and battery power is controlled based on Meter power"), //
	CHARGE_PV(2, "Charge Mode"), //
	DISCHARGE_PV(3, "Discharge Mode "), //
	IMPORT_AC(4, "Import Mode means buying power from grid"), //
	EXPORT_AC(5, "Export Mode means power export to grid,"), //
	CONSERVE(6, "Back-Up Mode"), //
	OFF_GRID(7, "cut off from gridconnection and turns to off-gridmode "), //
	BATTERY_STANDBY(8, "Battery Standby Mode "), //
	BUY_POWER(9, "Buying Mode"), //
	SELL_POWER(10, "Selling Mode"), //
	CHARGE_BAT(11, "Charge Mode"), //
	DISCHARGE_BAT(12, "DisCharging Mode"); //

	private final int value;
	private final String option;

	private PowerModeEMS(int value, String option) {
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