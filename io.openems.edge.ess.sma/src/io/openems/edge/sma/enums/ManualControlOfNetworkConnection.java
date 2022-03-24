package io.openems.edge.sma.enums;

import io.openems.common.types.OptionsEnum;

public enum ManualControlOfNetworkConnection implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	OFF(303, "Off"), //
	ON(308, "On"), //
	AUTOMATIC(1438, "Automatic");

	private final int value;
	private final String name;

	private ManualControlOfNetworkConnection(int value, String name) {
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