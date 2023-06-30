package io.openems.edge.batteryinverter.sinexcel.enums;

import io.openems.common.types.OptionsEnum;

public enum CpuType implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	SINGLE_CPU(0, "Single Cpu"), //
	DOUBLE_CPU(1, "Double Cpu");//

	private final int value;
	private final String name;

	private CpuType(int value, String name) {
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