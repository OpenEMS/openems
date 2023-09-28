package io.openems.edge.battery.fenecon.f2b.bmw.enums;

import io.openems.common.types.OptionsEnum;

public enum InsulationMeasurementStatus implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NOT_RUNNING(1, "Insulation measurement is not running"),//
	RUNNING(2, "Insulation measurement is running"),//
	;//

	private final int value;
	private final String name;

	private InsulationMeasurementStatus(int value, String name) {
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