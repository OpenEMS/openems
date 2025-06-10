package io.openems.edge.ess.fenecon.commercial40;

import io.openems.common.types.OptionsEnum;

public enum BatteryStringSwitchState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	MAIN_CONTATCTOR(1, "Main contactor"), //
	PRECHARGE_CONTACTOR(2, "Precharge contactor"), //
	FAN_CONTACTOR(4, "FAN contactor"), //
	BMU_POWER_SUPPLY_RELAY(8, "BMU power supply relay"), //
	MIDDLE_RELAY(16, "Middle relay");

	private final int value;
	private final String name;

	private BatteryStringSwitchState(int value, String name) {
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