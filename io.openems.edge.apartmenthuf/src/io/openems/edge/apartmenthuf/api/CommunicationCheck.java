package io.openems.edge.apartmenthuf.api;

import io.openems.common.types.OptionsEnum;

public enum CommunicationCheck implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	WAITING(0, "Waiting for signal"), //
	RECEIVED(1, "Signal received"); //

	private int value;
	private String name;

	private CommunicationCheck(int value, String name) {
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