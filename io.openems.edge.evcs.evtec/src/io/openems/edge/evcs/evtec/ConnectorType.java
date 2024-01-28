package io.openems.edge.evcs.evtec;

import io.openems.common.types.OptionsEnum;

public enum ConnectorType implements OptionsEnum {
	AC(0, "AC"), //
	CCS(1, "CCS"), //
	CHADEMO(2, "CHAdeMO"), //
	GBT(3, "GBT"), //
	UNDEFINED(-1, "Undefined") //
	;

	private final int value;
	private final String name;

	ConnectorType(int value, String name) {
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