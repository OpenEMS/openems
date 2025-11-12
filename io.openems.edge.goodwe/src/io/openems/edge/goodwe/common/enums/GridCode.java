package io.openems.edge.goodwe.common.enums;

import io.openems.common.types.OptionsEnum;

public enum GridCode implements OptionsEnum {
	UNDEFINED(-1, "Undefined", SafetyCountry.UNDEFINED), //
	VDE_4105(1, "VDE-AR-N 4105", SafetyCountry.GERMANY), //
	VDE_4110(2, "VDE-AR-N 4110", SafetyCountry.GERMANY), //
	;

	private final int value;
	private final String option;
	public final SafetyCountry relatedCountry;

	private GridCode(int value, String option, SafetyCountry relatedCountry) {
		this.value = value;
		this.option = option;
		this.relatedCountry = relatedCountry;
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