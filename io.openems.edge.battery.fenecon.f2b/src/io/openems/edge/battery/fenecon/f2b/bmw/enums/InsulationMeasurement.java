package io.openems.edge.battery.fenecon.f2b.bmw.enums;

import io.openems.common.types.OptionsEnum;

public enum InsulationMeasurement implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	DO_NOT_PERFORM_MEASUREMENT(0, "Do not perform"), //
	PERFORM_MEASUREMENT(1, "Perform"), //
	MEASUREMENT_REQUESTED(2, "Requested"), //
	INVALID(3, "Invalid"), //
	;

	private final int value;
	private final String name;

	private InsulationMeasurement(int value, String name) {
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