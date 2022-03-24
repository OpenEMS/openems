package io.openems.edge.sma.enums;

import io.openems.common.types.OptionsEnum;

public enum ReasonForGeneratorRequest implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NO_REQUEST(1773, "No Request"), //
	LOAD(1774, "Load"), //
	TIME_CONTROL(1775, "Time Control"), //
	MANUAL_ONE_HOUR(1776, "Manual One Hour"), //
	MANUAL_START(1777, "Manual Start"), //
	EXTERNAL_SOURCE(1778, "External Source"); //

	private final int value;
	private final String name;

	private ReasonForGeneratorRequest(int value, String name) {
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