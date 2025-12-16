package io.openems.edge.deye.enums;
import io.openems.common.types.OptionsEnum;

public enum RemoteMode implements OptionsEnum{
	UNDEFINED(-1, "Undefined"), //
	OFF(0, "Remote mode turned off"), //
	ON(1, "Remote mode turned on"); //
	

	private final int value;
	private final String name;

	private RemoteMode(int value, String name) {
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
