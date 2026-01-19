package io.openems.edge.ruhfass.battery.batcon.enums;

import io.openems.common.types.OptionsEnum;

public enum RemainingBusSimulationStatus implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	INACTIVE(0, "Remaining Bus Simulation Inactive"), //
	ACTIVE(1, "Remaining Bus Simulation Active"); //

	private int value;
	private String name;

	private RemainingBusSimulationStatus(int value, String name) {
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
