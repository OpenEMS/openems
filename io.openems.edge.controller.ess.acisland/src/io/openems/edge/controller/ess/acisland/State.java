package io.openems.edge.controller.ess.acisland;

import io.openems.common.types.OptionsEnum;

public enum State implements OptionsEnum {
	/**
	 * Unknown state on first start.
	 */
	UNDEFINED(-1, "Undefined"), //

	OFF_GRID(1, "Off-Grid"), //

	ON_GRID(2, "On-Grid"), //

	SWITCH_TO_OFFGRID(3, "Switch to Off-Grid"), //

	SWITCH_TO_ONGRID(4, "Switch to On-Grid");

	private final int value;
	private final String name;

	private State(int value, String name) {
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