package io.openems.edge.battery.fenecon.f2b.bmw.enums;

import io.openems.common.types.OptionsEnum;

public enum CoolingRequest implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NO_COOLING_REQUESTED(0, "No cooling requested"),//
	COOLING_REQUESTED(1, "Cooling requested"),//
	URGENT_COOLING_REQUESTED(2, "Urgent cooling requested"),//
	;//

	private final int value;
	private final String name;

	private CoolingRequest(int value, String name) {
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