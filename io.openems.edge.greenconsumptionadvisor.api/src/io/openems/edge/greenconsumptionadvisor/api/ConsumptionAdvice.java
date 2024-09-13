package io.openems.edge.greenconsumptionadvisor.api;

import io.openems.common.types.OptionsEnum;

public enum ConsumptionAdvice implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	GREEN(0, "Grid consumption recommended"), //
	YELLOW(1, "Reduce grid consumption"), //
	RED(2, "Avoid grid consumption");

	private final int value;
	private final String name;

	private ConsumptionAdvice(int value, String name) {
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
		return UNDEFINED;
	}
}