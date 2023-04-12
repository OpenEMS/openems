package io.openems.edge.solaredge.enums;

import io.openems.common.types.OptionsEnum;

public enum BatteryStatus implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	SE_BATT_STATUS_OFF(0, "Off"), //
	SE_BATT_STATUS_STBY(1, "Standby"), //
	SE_BATT_STATUS_INIT(2, "Init"), //
	SE_BATT_STATUS_CHARGE(3, "Charge"), //
	SE_BATT_STATUS_DISCHARGE(4, "Discharge"), //
	SE_BATT_STATUS_FAULT(5, "Fault"), //
	// 6 doesn´t exist
	SE_BATT_STATUS_IDLE(7, "Idle"); //

	private final int value;
	private final String name;

	private BatteryStatus(int value, String name) {
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
