package io.openems.edge.sma.enums;

import io.openems.common.types.OptionsEnum;

public enum PvMainsConnection implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	DISCONNECTED(1779, "Disconnected"), //
	UTILITY_GRID(1780, "Utility Grid"), //
	STAND_ALONE_GRID(1781, "Stand-Alone Grid"); //

	private final int value;
	private final String name;

	private PvMainsConnection(int value, String name) {
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