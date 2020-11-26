package io.openems.edge.goodwe.common.enums;

import io.openems.common.types.OptionsEnum;

public enum OutputTypeAC implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	SINGLE_PHASE(0, "single phase"), //
	THREE_PHASE_FOUR_WIRE(1, "three phase four wire system"), //
	THREE_PHASE_THREE_WIRE(2, "three phase three wire system");

	private final int value;
	private final String option;

	private OutputTypeAC(int value, String option) {
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