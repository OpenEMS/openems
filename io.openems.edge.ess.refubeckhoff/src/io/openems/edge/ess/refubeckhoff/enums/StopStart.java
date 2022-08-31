package io.openems.edge.ess.refubeckhoff.enums;

import io.openems.common.types.OptionsEnum;

public enum StopStart implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	STOP(0, "Stop"), //
	START(1, "Start");

	private int value;
	private String option;

	private StopStart(int value, String option) {
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
