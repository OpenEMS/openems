package io.openems.edge.ruhfass.battery.batcon.enums;

import io.openems.common.types.OptionsEnum;

public enum BalancingMessage implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NO_FAILURE(0, "No Failure during last Balancing process"), //
	BALANCING_PROCESS_STOPPED(1, "Balancing process stopped"); //

	private int value;
	private String name;

	private BalancingMessage(int value, String name) {
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
