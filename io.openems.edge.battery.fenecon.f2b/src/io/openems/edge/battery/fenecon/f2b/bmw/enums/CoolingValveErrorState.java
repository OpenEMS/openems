package io.openems.edge.battery.fenecon.f2b.bmw.enums;

import io.openems.common.types.OptionsEnum;

public enum CoolingValveErrorState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	OK(0, "Ok"),//
	SHORT_TO_GROUND(0x1, "Short to ground"),//
	SHORT_TO_U_BAT(0x2, "Short to voltage battery"),//
	BROKEN_CONDUCTION(0x3, "Broken Conduction"),//
	DRIVER_ERROR(0x6, "Driver error"),//
	VALVE_CAN_NOT_BE_CLOSED(0x9, "Valve can not be closed"),//
	VALVE_CAN_NOT_BE_OPENED(0xC, "Valve can not be opened"),//
	;//

	private final int value;
	private final String name;

	private CoolingValveErrorState(int value, String name) {
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