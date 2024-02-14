package io.openems.edge.deye.batteryinverter.enums;

import io.openems.common.types.OptionsEnum;

public enum BmsProtocolSelection implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	ENERGY_PORT(0, "EP"), //
	ALPHA(1, "Alpha-Ess"), //
	PYLONTECH(2, "Pylontech"), //
	BMSER(3, "Bmser"); //

	private final int value;
	private final String name;

	private BmsProtocolSelection(int value, String name) {
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