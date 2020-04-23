package io.openems.edge.battery.soltaro;

import io.openems.common.types.OptionsEnum;

public enum ChargeIndication implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	STANDBY(0, "Standby"), //
	DISCHARGE(1, "Dischare"), //
	CHARGE(2, "Charge");

	private int value;
	private String name;

	private ChargeIndication(int value, String name) {
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