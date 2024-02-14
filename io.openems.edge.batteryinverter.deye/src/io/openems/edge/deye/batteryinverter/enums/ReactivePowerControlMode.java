package io.openems.edge.deye.batteryinverter.enums;

import io.openems.common.types.OptionsEnum;

public enum ReactivePowerControlMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	CONSTANT_REACTIVE_POWER(0, "Constant Reactive Power"), //
	VOLT_VAR_ENABLED(1, "Volt Var Enabled"), //
	CONSTANT_PF(2, "Constanr Power Factor"), //
	WATT_PF_ENABLED(3, "Watt Power Factor Enabled"); //

	private final int value;
	private final String name;

	private ReactivePowerControlMode(int value, String name) {
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