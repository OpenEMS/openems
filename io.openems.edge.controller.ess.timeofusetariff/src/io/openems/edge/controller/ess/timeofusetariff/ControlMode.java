package io.openems.edge.controller.ess.timeofusetariff;

import io.openems.common.types.OptionsEnum;

public enum ControlMode implements OptionsEnum {
	CHARGE_CONSUMPTION(0, "Charge consumption from from grid"), //
	DELAY_DISCHARGE(1, "Dealys discharge for high price hours"); //

	private final int value;
	private final String name;

	private ControlMode(int value, String name) {
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
		return CHARGE_CONSUMPTION;
	}
}