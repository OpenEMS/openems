package io.openems.edge.ess.stabl.enums;

import io.openems.common.types.OptionsEnum;

public enum ActivatePowerStage implements OptionsEnum {

	UNDEFINED(-1, "Undefined"), //
	POWER_STAGE_ON(1, "power stage on"), //
	POWER_STAGE_OFF(0, "Power stage off"), //

	;

	private final int value;
	private final String name;

	private ActivatePowerStage(int value, String name) {
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
