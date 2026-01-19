package io.openems.edge.common.oauth;

import io.openems.common.types.OptionsEnum;

public enum ConnectionState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NOT_CONNECTED(0, "Not connected"), //
	EXPIRED(1, "Expired"), //
	VALIDATING(2, "Validating"), //
	CONNECTED(3, "Connected"), //
	;

	private final int value;
	private final String name;

	ConnectionState(int value, String name) {
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
		return ConnectionState.UNDEFINED;
	}
}