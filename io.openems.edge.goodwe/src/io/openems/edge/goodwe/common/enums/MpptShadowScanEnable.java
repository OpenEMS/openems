package io.openems.edge.goodwe.common.enums;

import io.openems.common.types.OptionsEnum;

public enum MpptShadowScanEnable implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	OFF(0, "MPPT shadow scan enable"), //
	ON(1, "MPPT shadow scan disable");//

	private final int value;
	private final String option;

	private MpptShadowScanEnable(int value, String option) {
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