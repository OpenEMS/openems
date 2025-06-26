package io.openems.edge.kaco.blueplanet.hybrid10.ess;

import io.openems.common.types.OptionsEnum;

public enum PowerManagementConfiguration implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	EXTERNAL_EMS(0, "External EMS"), //
	BATTERY_CHARGING(1, "Battery Charging"), //
	SELF_CONSUMPTION(2, "Self Consumption"), //
	MAX_YIELD(3, "Max. Yield");

	private final int value;
	private final String name;

	private PowerManagementConfiguration(int value, String name) {
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