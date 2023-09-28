package io.openems.edge.battery.fenecon.f2b.bmw.enums;

import io.openems.common.types.OptionsEnum;

public enum HvContactorStatus implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	CONTACTORS_OPENED(0, "Contactors opened"), //
	PRECONDITIONING(1, "Preconditioning"), //
	CONTACTORS_CLOSED(2, "Contactors closed"),//
	;//

	private final int value;
	private final String name;

	private HvContactorStatus(int value, String name) {
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