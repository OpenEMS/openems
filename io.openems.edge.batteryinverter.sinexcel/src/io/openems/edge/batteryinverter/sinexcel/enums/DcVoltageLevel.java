package io.openems.edge.batteryinverter.sinexcel.enums;

import io.openems.common.types.OptionsEnum;

public enum DcVoltageLevel implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	V_750(0, "750 V"), //
	V_830(1, "830 V");//

	private final int value;
	private final String name;

	private DcVoltageLevel(int value, String name) {
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