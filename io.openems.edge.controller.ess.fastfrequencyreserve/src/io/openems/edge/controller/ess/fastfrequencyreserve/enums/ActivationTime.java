package io.openems.edge.controller.ess.fastfrequencyreserve.enums;

import io.openems.common.types.OptionsEnum;

public enum ActivationTime implements OptionsEnum {
	SHORT_ACTIVATION_RUN(700, "Short activation time run, 700 in milliseconds"), //
	MEDIUM_ACTIVATION_RUN(1000, "Medium activation time run, 1000 in milliseconds"), //
	LONG_ACTIVATION_RUN(1300, "Long activation time run, 1300 in milliseconds");

	private final int value;
	private final String name;

	private ActivationTime(int value, String name) {
		this.value = value;
		this.name = name;
	}

	public int getValue() {
		return this.value;
	}

	public String getName() {
		return this.name;
	}

	@Override
	public OptionsEnum getUndefined() {
		return LONG_ACTIVATION_RUN;
	}
}
