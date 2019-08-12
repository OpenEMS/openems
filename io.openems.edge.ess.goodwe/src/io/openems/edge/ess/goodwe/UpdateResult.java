package io.openems.edge.ess.goodwe;

import io.openems.common.types.OptionsEnum;

enum UpdateResult implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	OK(0, "Bin file OK"), //
	SUCCESS(1, "Updata Success"), //
	NG(14, "Bin file NG"), //
	FAIL(15, "Updata Fail");

	private int value;
	private String option;

	private UpdateResult(int value, String option) {
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
		return UNDEFINED;
	}	
}