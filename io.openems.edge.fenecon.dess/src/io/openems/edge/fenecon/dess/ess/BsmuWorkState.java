package io.openems.edge.fenecon.dess.ess;

import io.openems.common.types.OptionsEnum;

public enum BsmuWorkState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	INIT(0, "Initialization"), //
	OFF_GRID(1, "Off-Grid"), //
	BEING_ON_GRID(2, "Being On-Grid"), //
	ON_GRID(3, "On-Grid"), //
	BEING_STOP(4, "Being Stop"), //
	FAULT(5, "Fault"), //
	DEBUG(6, "Debug-Mode"), //
	LOW_CONSUMPTION(8, "Low-Consumption-Mode"), //
	BEING_PRE_CHARGE(9, "Being Pre-Charge"), //
	PRE_CHARGE(10, "Pre-Charging");

	private final int value;
	private final String name;

	private BsmuWorkState(int value, String name) {
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