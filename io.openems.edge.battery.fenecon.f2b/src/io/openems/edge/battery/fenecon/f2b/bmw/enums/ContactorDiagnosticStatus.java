package io.openems.edge.battery.fenecon.f2b.bmw.enums;

import io.openems.common.types.OptionsEnum;

public enum ContactorDiagnosticStatus implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NO_CONTACTOR_STUCK(0, "No contactor stuck"), //
	ONE_CONTACTOR_STUCK(1, "One contactor stuck"), //
	TWO_CONTACTOR_STUCK(2, "Two contactor stuck"),//
	;//

	private final int value;
	private final String name;

	private ContactorDiagnosticStatus(int value, String name) {
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