package io.openems.edge.batteryinverter.refu88k;

import io.openems.common.types.OptionsEnum;

public enum WMaxLimEna implements OptionsEnum {

	UNDEFINED(-1, "Undefined"),
	DISABLED(0, "Disabled"),
	ENABLED(1, "Enabled")
	;

	private final int value;
	private final String name;

	private WMaxLimEna(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override
	public int getValue() {
		return value;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
	
	
	
}
