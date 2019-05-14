package io.openems.edge.battery.renaultzoe;

import io.openems.common.types.OptionsEnum;

public enum BatteryChargeDischargeRequest implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	CHARGE(0, "Charge"), //
	DISCHARGE(1, "Discharge"); //

	
	private int value;
	private String name;

	private BatteryChargeDischargeRequest(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override
	public int getValue() {
		return value;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}
