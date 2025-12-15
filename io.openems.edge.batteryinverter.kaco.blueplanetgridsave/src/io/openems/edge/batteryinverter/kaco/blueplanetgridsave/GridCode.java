package io.openems.edge.batteryinverter.kaco.blueplanetgridsave;

import io.openems.common.types.OptionsEnum;

public enum GridCode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	VDE_4105(1, "VDE-AR-N 4105"), //
	VDE_4110(2, "VDE-AR-N 4110"), //
	;

	private final int value;
	private final String option;

	private GridCode(int value, String option) {
		this.value = value;
		this.option = option;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.option;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}