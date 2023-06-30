package io.openems.edge.controller.ess.timeofusetariff.discharge;

import io.openems.common.types.OptionsEnum;

public enum StateMachine implements OptionsEnum {

	DELAYED(0, "Delayed"), //
	ALLOWS_DISCHARGE(1, "No active limitation, discharge is allowed"), //
	STANDBY(2, "Outside controller time limits"); //

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
		return STANDBY;
	}

}
