package io.openems.edge.ess.goodwe;

import io.openems.common.types.OptionsEnum;

enum ACR_MeterType implements OptionsEnum {
	UNKNOWN(0, "Unknown"), //
	ACR_1_Phase(1, "ACR 1 Phase"), //
	ACR_3_Phase(2, "ACR 3 Phase"); //

	private int value;
	private String option;

	private ACR_MeterType(int value, String option) {
		this.value = value;
		this.option = option;
	}

	@Override
	public int getValue() {
		return value;
	}

	@Override
	public String getName() {
		return option;
	}
	
	@Override
	public OptionsEnum getUndefined() {
		return UNKNOWN;
	}	
}