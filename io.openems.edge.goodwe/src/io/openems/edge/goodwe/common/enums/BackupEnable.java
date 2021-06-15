package io.openems.edge.goodwe.common.enums;

import io.openems.common.types.OptionsEnum;

public enum BackupEnable implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	OFF(0, "Backup output Off"), //
	ON(1, "Backup output On");//

	private final int value;
	private final String option;

	private BackupEnable(int value, String option) {
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