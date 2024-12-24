package io.openems.edge.battery.bmw.statemachine;

import io.openems.common.types.OptionsEnum;

public enum GoRunningSubState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	CHECK_BCS_POWER_STATE(0, "Check BCS"), //
	ACTIVATE_BCS_POWER_STATE(1, "Activate BCS power state"), //
	ACTIVATE_INVERTER_RELEASE(2, "Activate inverter release"), //
	CHECK_BCS_INVERTER_RELEASE(3, "Check BCS after activation"), //
	START_BATTERY(4, "Start battery"), //
	FINISHED(5, "Finished"), //
	ERROR(6, "Error");//

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
