package io.openems.edge.battery.fenecon.f2b.bmw.enums;

import io.openems.common.types.OptionsEnum;

public enum GoRunningSubState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	ENABLE_CAN_COMMUNICATION(0, "Enable CAN communication"), //
	TOGGLE_TERMINAL_15_HW(1, "Toggle terminal 15 hw "), //
	CLOSE_HV_CONTACTORS(2, "Close hv contactors"), //
	FINISHED(3, "Finished"), //
	ERROR(4, "Error");//

	private final int value;
	private final String name;

	private GoRunningSubState(int value, String name) {
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
