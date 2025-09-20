package io.openems.edge.controller.evcs;

import io.openems.common.types.OptionsEnum;

/**
 * The Priorities for charging. Which Component should be preferred.
 */
public enum Priority implements io.openems.common.types.OptionsEnum {

	CAR(0, "Car"), STORAGE(1, "Storage");

	private final int value;
	private final String name;

	private Priority(int value, String name) {
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
		return CAR;
	}
}
