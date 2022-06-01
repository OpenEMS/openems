package io.openems.edge.battery.fenecon.commercial;

import io.openems.common.types.OptionsEnum;

public enum TemperaturePosition implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	BALANCE_BOARD(0, "Balance Board"), //
	TEMPERATURE_BOARD(1, "Temperature Board") //
	;

	private final int value;
	private final String name;

	private TemperaturePosition(int value, String name) {
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