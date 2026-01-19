package io.openems.edge.ruhfass.battery.batcon.enums;

import io.openems.common.types.OptionsEnum;

public enum IsolationMearsurementDeactivationModeRead implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	INACTIVE(0, "Isolation Measurement Inactive"), //
	ACTIVE(1, "Isolation Measurement Active"), //
	CONTROL_ISOLATIONMONITOR_NOT_SUPPORTED(255, "Control Isolationmonitor not supported"); //

	private int value;
	private String name;

	private IsolationMearsurementDeactivationModeRead(int value, String name) {
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
