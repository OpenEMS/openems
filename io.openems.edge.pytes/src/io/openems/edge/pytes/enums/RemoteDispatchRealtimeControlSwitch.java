package io.openems.edge.pytes.enums;

import io.openems.common.types.OptionsEnum;

// ToDo
public enum RemoteDispatchRealtimeControlSwitch implements OptionsEnum {
	UNDEFINED(-1, "Undefined"),
	DISABLE(0, "No limitation"),
	BATTERY_STANDBY(1, "Standby. No charge/discharge operation"),
	BATTERY_CONTROL(2, "Battery charge/discharge control"),
	GRID_POINT_CONTROL(3, "Grid connection point Import/Export control"),
	AC_GRID_POINT_CONTROL(4, "AC Grid Port Import/Export Control");
	private final int value;
	private final String name;

	RemoteDispatchRealtimeControlSwitch(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override public int getValue() { return this.value; }
	@Override public String getName() { return this.name; }
	@Override public OptionsEnum getUndefined() { return UNDEFINED; }
}
