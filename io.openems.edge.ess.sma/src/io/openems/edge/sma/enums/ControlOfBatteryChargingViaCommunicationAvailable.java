package io.openems.edge.sma.enums;

import io.openems.common.types.OptionsEnum;

public enum ControlOfBatteryChargingViaCommunicationAvailable implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	YES(1129, "Yes"), //
	NO(1130, "No");

	private final int value;
	private final String name;

	private ControlOfBatteryChargingViaCommunicationAvailable(int value, String name) {
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