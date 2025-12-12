package io.openems.edge.deye.enums;

import io.openems.common.types.OptionsEnum;
// register 500
public enum BatteryRunState implements OptionsEnum {
    UNDEFINED(-1, "Undefined"),
    STANDBY(0, "Standby"),
    NORMAL(1, "Normal"),
    WARNING(2, "Warning"),
    ERROR(3, "Error"), 
    INITIALIZING(4, "Not initialized"),
    OFFLINE(5,"Battery not connected"),
    NO_BATTERY(6, "No battery configured")
    
    
    ;

	private final int value;
	private final String name;

	private BatteryRunState(int value, String name) {
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
