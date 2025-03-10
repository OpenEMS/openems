package io.openems.edge.thermometer.esera.onewire.enums;

import io.openems.common.types.OptionsEnum;

public enum OwdStatus implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NORMAL(0, "Normal"), // SoC in range between min and max
	ERROR(5, "Sensor unreadable"), //
	NO_SENSOR(10, "No sensor available");//
	
	private final int value;
	private final String name;

	private OwdStatus(int value, String name) {
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


