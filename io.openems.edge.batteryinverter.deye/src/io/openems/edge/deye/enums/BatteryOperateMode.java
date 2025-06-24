package io.openems.edge.deye.enums;

import io.openems.common.types.OptionsEnum;

public enum BatteryOperateMode implements OptionsEnum {
    UNDEFINED(-1, "Undefined"),
    VOLTAGE(0, "Operates according to voltage"),
    CAPACITY(1, "Operates according to capacity"),
    NO_BATTERY(2, "No battery");   

	private final int value;
	private final String name;

	private BatteryOperateMode(int value, String name) {
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


