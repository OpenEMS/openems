package io.openems.edge.controller.ess.sohcycle;

import io.openems.common.types.OptionsEnum;

public enum BatteryBalanceStatus implements OptionsEnum {
	NOT_MEASURED(0), //
	BALANCED(1), //
	NOT_BALANCED(2), //
	ERROR(3);

	private final int value;

	BatteryBalanceStatus(int value) {
		this.value = value;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.name();
	}

	@Override
	public OptionsEnum getUndefined() {
		return NOT_MEASURED;
	}
}

