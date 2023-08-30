package io.openems.edge.system.fenecon.industrial.l.envicool;

import io.openems.common.types.OptionsEnum;

public enum RuntimeControlMode implements OptionsEnum {

	UNDEFINED(-1, "Undefined"), //
	WATER_INLET(0, "Water inlet"), //
	WATER_OUTLET_SELECT(1, "Water outlet select"), //
	MONITOR_TEMP(3, "Monitor temp when controlling the air con using the monitored temp"), //
	;

	private final int value;
	private final String name;

	private RuntimeControlMode(int value, String name) {
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