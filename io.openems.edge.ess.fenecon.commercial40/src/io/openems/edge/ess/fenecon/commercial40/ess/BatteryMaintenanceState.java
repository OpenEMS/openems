package io.openems.edge.ess.fenecon.commercial40.ess;

import io.openems.common.types.OptionsEnum;

public enum BatteryMaintenanceState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	OFF(0, "Off"), //
	ON(1, "On"); //

	private final int value;
	private final String name;

	private BatteryMaintenanceState(int value, String name) {
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