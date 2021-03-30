package io.openems.edge.batteryinverter.refu88k;

import io.openems.common.types.OptionsEnum;

public enum DerTyp implements OptionsEnum {
	UNDEFINED(-1, "Undefined"),
	PV(4, "PV"),
	PV_STOR(82, "PVStor")
	;

	private final int value;
	private final String name;

	private DerTyp(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override
	public int getValue() {
		return value;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
	
}
