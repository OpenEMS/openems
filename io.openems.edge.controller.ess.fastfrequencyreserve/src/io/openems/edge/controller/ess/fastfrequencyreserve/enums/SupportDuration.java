package io.openems.edge.controller.ess.fastfrequencyreserve.enums;

import io.openems.common.types.OptionsEnum;

public enum SupportDuration implements OptionsEnum {
	SHORT_SUPPORT_DURATION(5, "long support duration 5 seconds"),
	LONG_SUPPORT_DURATION(30, "long support duration 30 seconds");

	private final int value;
	private final String name;

	private SupportDuration(int value, String name) {
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
		return LONG_SUPPORT_DURATION;
	}

}
