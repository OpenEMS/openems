package io.openems.edge.edge2edge.websocket.bridge;

import io.openems.common.types.OptionsEnum;

public enum ConnectionStateOption implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NOT_CONNECTED(0, "Not Connected"), //
	CONNECTING(1, "Connecting"), //
	AUTHENTICATING(2, "Authenticating"), //
	CONNECTED(3, "Connected"), //
	;

	private final int value;
	private final String name;

	private ConnectionStateOption(int value, String name) {
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
		return ConnectionStateOption.UNDEFINED;
	}

}