package io.openems.edge.goodwe.common.enums;

import io.openems.common.types.OptionsEnum;

public enum GoodweGridMeterType implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	SINGLE_PHASE(0, "Single phase grid meter"), //
	THREE_PHASE_THREE_WIRE(1, "Three phase three wire grid meter"), //
	THREE_PHASE_FOUR_WIRE(2, "Three phase four wire grid meter"), //
	HOME_KIT(3, "Home kit");
	// TODO 4 ?

	private final int value;
	private final String option;

	private GoodweGridMeterType(int value, String option) {
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