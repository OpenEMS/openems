package io.openems.edge.ruhfass.battery.batcon.enums;

import io.openems.common.types.OptionsEnum;

public enum BalancingMonitoring implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NO_BALANCING(0, "No Balancing"), //
	BALANCING_NOT_POSSIBLE(1, "Balancing not possible"), //
	BALANCING_ACTIVE(2, "Balancing active"); //

	private int value;
	private String name;

	private BalancingMonitoring(int value, String name) {
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
