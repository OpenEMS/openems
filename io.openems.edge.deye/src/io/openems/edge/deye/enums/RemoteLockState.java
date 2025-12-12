package io.openems.edge.deye.enums;

import io.openems.common.types.OptionsEnum;
public enum RemoteLockState implements OptionsEnum{
	UNDEFINED(-1, "Undefined"), //
	TURNED_ON(0, "Remote Lock turned on"), //
	TURNED_OFF(2, "Remote Lock turned off"); //

	private final int value;
	private final String name;

	private RemoteLockState(int value, String name) {
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
