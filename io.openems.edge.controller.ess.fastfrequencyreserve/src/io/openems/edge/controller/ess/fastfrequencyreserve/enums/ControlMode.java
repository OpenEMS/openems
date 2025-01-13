package io.openems.edge.controller.ess.fastfrequencyreserve.enums;

import io.openems.common.types.OptionsEnum;

public enum ControlMode implements OptionsEnum {
	MANUAL_ON(0, "Manual control for the ON signal, FFR is swtiched on"), //
	MANUAL_OFF(1, "Manual control for the OFF signal, FFR is swtiched off") //
	; //

	private final int value;
	private final String name;

	private ControlMode(int value, String name) {
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
		return MANUAL_OFF;
	}
}
