package io.openems.edge.ruhfass.battery.batcon.enums;

import io.openems.common.types.OptionsEnum;

public enum RemainingBusSimulationCommand implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	OFF(0, "Off"), //
	ON(1, "On"); //

	private int value;
	private String name;

	private RemainingBusSimulationCommand(int value, String name) {
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
