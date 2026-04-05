package io.openems.edge.ruhfass.battery.batcon.enums;

import io.openems.common.types.OptionsEnum;

public enum IsolationMearsurementDeactivationModeWrite implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	DEACTIVATE(0, "Isolation Measurement Deactivate"), //
	ACTIVATE(1, "Isolation Measurement Activate"); //

	private int value;
	private String name;

	private IsolationMearsurementDeactivationModeWrite(int value, String name) {
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
