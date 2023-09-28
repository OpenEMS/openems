package io.openems.edge.battery.fenecon.f2b.bmw.enums;

import io.openems.common.types.OptionsEnum;

public enum HeatingRequest implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NOT(0, "Not"), //
	HEAT_UP(1, "Heat up"), //
	ELECTRICAL_HEATING(2, "Electrical heating"), //
	COOLING_DOWN(3, "Cooling down"), //
	ELECTRICAL_COOLING(4, "Electrical cooling"), //
	COATING(5, "Coating"), //
	DEFROST(6, "Defrost"), //
	BATTERY_COOLING(7, "Battery cooling"), //
	URGENT_BATTERY_COOLING(8, "Urgent battery cooling"), //
	BATTERY_HEATING_CHARGING(9, "Battery heating charging"), //
	PRECONDTIONING_1(0xA, "preconditioning 1"), //
	PRECONDTIONING_2(0xB, "preconditioning 2"), //
	PRECONDTIONING_3(0xC, "preconditioning 3"), //
	PRECONDTIONING_4(0xD, "preconditioning 4"), //
	BATTERY_HEATING_PRECONDITIONING(0xE, "Battery heating preconditioning"),//
	;//

	private final int value;
	private final String name;

	private HeatingRequest(int value, String name) {
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