package io.openems.edge.ruhfass.battery.batcon.enums;

import io.openems.common.types.OptionsEnum;

public enum IsolationResistanceStatus implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	INIT(0, "Init"), //
	NOT_VALID(1, "Not valid"), MEASUREMENT_ACTIVE(2, "Measurement active"), //
	INSULATION_DEFECT(3, "Insulation defect"), //
	VALID(4, "Valid"); //

	private int value;
	private String name;

	private IsolationResistanceStatus(int value, String name) {
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
