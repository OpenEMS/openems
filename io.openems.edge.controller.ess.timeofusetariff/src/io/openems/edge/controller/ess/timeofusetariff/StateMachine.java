package io.openems.edge.controller.ess.timeofusetariff;

import io.openems.common.types.OptionsEnum;

public enum StateMachine implements OptionsEnum {

	DELAY_DISCHARGE(0, "Delaying the discharge from the battery is scheduled"), //
	ALLOWS_DISCHARGE(1,
			"No active limitation, Discharge is permitted due to high-price hour or when ample self-generated energy is available"), //
	CHARGE_FROM_PV(2,
			"No active limitation set, Excess PV energy predicted and battery can charge from surplus PV or feed energy to the grid based on its state of charge (SoC)"), //
	CHARGE_FROM_GRID(3, "Charging the battery from the grid scheduled"); //

	private final int value;
	private final String name;

	private StateMachine(int value, String name) {
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
		return CHARGE_FROM_PV;
	}

}
