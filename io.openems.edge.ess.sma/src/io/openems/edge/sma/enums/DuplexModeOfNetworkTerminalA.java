package io.openems.edge.sma.enums;

import io.openems.common.types.OptionsEnum;

public enum DuplexModeOfNetworkTerminalA implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NOT_CONNECTED(1725, "Not Connected"), //
	HALF_DUPLEX(1726, "Half Duplex"), //
	FULL_DUPLEX(1727, "Full Duplex");

	private final int value;
	private final String name;

	private DuplexModeOfNetworkTerminalA(int value, String name) {
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