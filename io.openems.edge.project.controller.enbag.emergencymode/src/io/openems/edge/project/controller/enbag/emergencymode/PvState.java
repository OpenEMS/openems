package io.openems.edge.project.controller.enbag.emergencymode;

import io.openems.edge.common.channel.doc.OptionsEnum;

public enum PvState implements OptionsEnum {
	/**
	 * Unknown state on first start.
	 */
	UNDEFINED(-1, "Undefined"), //

	PV_LOW(0, "Pv Power Is Less Than 3kW "), //

	PV_OKAY(1, "Pv Power Is Between 3kW and 35kW"), //

	PV_HIGH(1, "Pv Power Is More Than 35kW");

	private final int value;
	private final String name;

	private PvState(int value, String name) {
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