package io.openems.edge.ess.sma.enums;

import io.openems.common.types.OptionsEnum;

public enum BatteryState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	OFF(303, "Off"), //
	STANDBY(2291, "Standby"), //
	EMERGENCY_CHARGE(3664, "Emergency Charge"), //
	CHARGE(2292, "Charge"), //
	DISCHARGE(2293, "Discharge"), //
	;

	private final int value;
	private final String name;

	private BatteryState(int value, String name) {
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
