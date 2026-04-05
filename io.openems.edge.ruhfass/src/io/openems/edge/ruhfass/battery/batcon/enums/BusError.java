package io.openems.edge.ruhfass.battery.batcon.enums;

import io.openems.common.types.OptionsEnum;

public enum BusError implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	INACTIVE(0, "Bus Error Inactive"), //
	ACTIVE(1, "Bus Error Active"); //

	private int value;
	private String name;

	private BusError(int value, String name) {
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
