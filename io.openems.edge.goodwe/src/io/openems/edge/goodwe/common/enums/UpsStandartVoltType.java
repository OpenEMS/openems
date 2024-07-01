package io.openems.edge.goodwe.common.enums;

import io.openems.common.types.OptionsEnum;

public enum UpsStandartVoltType implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	VOLT_208(0, "208 V"), //
	VOLT_220(1, "220 V "), //
	VOLT_240(2, "240 V"), //
	VOLT_230(3, "230 V"),//
	;

	private final int value;
	private final String option;

	private UpsStandartVoltType(int value, String option) {
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