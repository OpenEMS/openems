package io.openems.edge.ruhfass.battery.batcon.enums;

import io.openems.common.types.OptionsEnum;

public enum StatusKL15Can implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	KL15_OFF(0, "KL15 Off"), //
	KL15_ON(1, "KL15 On"); //

	private int value;
	private String name;

	private StatusKL15Can(int value, String name) {
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
