package io.openems.edge.pytes.enums;

import io.openems.common.types.OptionsEnum;

public enum RemoteDispatchRealtimeControlSwitch implements OptionsEnum {
	UNDEFINED(-1, "Undefined"),
	DISABLE(0, "No limitation"),
	BATTERY_STANDBY(1, "Standby. No charge/discharge operation"),
	/** Reg 44105 = 2: the EMS sets the battery power (reg 44106, negative = discharge). */
	BATTERY_CONTROL(2, "Battery charge/discharge control: EMS sets the battery power (recommended)"),
	/** Reg 44105 = 3: the inverter regulates the grid connection point via its own meter. Not usable with OpenEMS. */
	GRID_POINT_CONTROL(3, "Grid connection point control via the inverter's own meter (not for OpenEMS)"),
	/** Reg 44105 = 4: the inverter regulates its own AC output (incl. PV) to reg 44106, positive = export. */
	AC_OUTPUT_CONTROL(4, "Inverter AC output control: inverter regulates its AC power incl. PV to the set-point");
	private final int value;
	private final String name;

	RemoteDispatchRealtimeControlSwitch(int value, String name) {
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
