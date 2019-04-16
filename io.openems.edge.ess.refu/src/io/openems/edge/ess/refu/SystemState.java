package io.openems.edge.ess.refu;

import io.openems.common.types.OptionsEnum;

enum SystemState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	OFF(0, "Off"), //
	INIT(1, "Init"), //
	PRE_OPERATION(2, "Pre-operation"), //
	STANDBY(3, "Standby"), //
	OPERATION(4, "Operation"), //
	ERROR(5, "Error");

	private int value;
	private String option;

	private SystemState(int value, String option) {
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