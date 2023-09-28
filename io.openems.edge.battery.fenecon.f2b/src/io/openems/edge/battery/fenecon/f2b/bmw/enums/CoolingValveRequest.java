package io.openems.edge.battery.fenecon.f2b.bmw.enums;

import io.openems.common.types.OptionsEnum;

public enum CoolingValveRequest implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NO_REQUEST(0, "No Request"),//
	OPEN(1, "Request to open cooling valve"),//
	OPEN_ALSO_ADDITONAL_COOLING(2, "Request to open cooling valve and additonal cooling"),//
	;//

	private final int value;
	private final String name;

	private CoolingValveRequest(int value, String name) {
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