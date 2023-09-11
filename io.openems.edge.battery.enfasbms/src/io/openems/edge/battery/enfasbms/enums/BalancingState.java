package io.openems.edge.battery.enfasbms.enums;

import io.openems.common.types.OptionsEnum;

public enum BalancingState implements OptionsEnum {

	UNDEFINED(-1, "Undefined"), //
	BALANCE_NOT_REQUIRED(0, "Balancing not required"), //
	BALANCE_REQUIRED(1, "Balancing required"), //
	BALANCE_ACTIVE(2, "Balancing active"), //
	BALANCE_ERROR(3, "Balancing error"), //
	BALANCING_NOT_ACTIVE(4, "Balancing not active");

	private final int value;
	private final String name;

	private BalancingState(int value, String name) {
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
