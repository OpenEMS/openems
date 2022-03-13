package io.openems.edge.sma.enums;

import io.openems.common.types.OptionsEnum;

public enum MultifunctionRelayStatus implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	CLOSED(51, "Closed"), //
	OPEN(311, "Open"); //

	private final int value;
	private final String name;

	private MultifunctionRelayStatus(int value, String name) {
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