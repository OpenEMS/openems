package io.openems.edge.deye.enums;

import io.openems.common.types.OptionsEnum;
public enum DisableEnable implements OptionsEnum{
	UNDEFINED(-1, "Undefined"), //
	ENABLED(0, "Enabled"), //
	DISABLED(1, "Disabled"); //
	

	private final int value;
	private final String name;

	private DisableEnable(int value, String name) {
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
