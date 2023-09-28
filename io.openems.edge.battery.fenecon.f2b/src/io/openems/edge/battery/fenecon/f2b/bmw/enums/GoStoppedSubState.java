package io.openems.edge.battery.fenecon.f2b.bmw.enums;

import io.openems.common.types.OptionsEnum;

public enum GoStoppedSubState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	WAIT_FOR_CURRENT_REDUCTION(0, "Wait for current reduction"), //
	OPEN_HV_CONTACTORS(1, "Open hv contactors"), //
	POWER_OFF_F2B_TERMINAL_15_SW_AND_HW(2, "Power off f2b terminal 15 sw and hw"), //
	WAIT_FOURTY_SECONDS(3, "Wait fourty seconds"), //
	F2B_TERMINAL_30C_SWITCH_OFF(4, "F2B terminal 30c switch off"), //
	FINISHED(5, "Finished"), //
	ERROR(6, "Error");//

	private final int value;
	private final String name;

	private GoStoppedSubState(int value, String name) {
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
