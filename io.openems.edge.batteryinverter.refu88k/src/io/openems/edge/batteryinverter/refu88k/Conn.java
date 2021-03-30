package io.openems.edge.batteryinverter.refu88k;

import io.openems.common.types.OptionsEnum;

public enum Conn implements OptionsEnum {
	UNDEFINED(-1, "Undefined"),
	DISCONNECT(0, "Disconnect"),
	CONNECT(1, "Connect")
	;

	private final int value;
	private final String name;

	private Conn(int value, String name) {
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
