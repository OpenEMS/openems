package io.openems.edge.ess.sungrow.enums;

import io.openems.common.types.OptionsEnum;

public enum EmsMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	SELF_CONSUMPTION(0, "Self-consumption mode"), //
	FORCED_MODE(2, "Forced Mode"), //
	EXTERNAL_EMS_MODE(3, "External EMS mode") //
	;

	private final int value;
	private final String name;

	private EmsMode(int value, String name) {
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
