package io.openems.edge.sma.enums;

import io.openems.common.types.OptionsEnum;

public enum DataTransferRateOfNetworkTerminalA implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	TEN_MBIT(1720, "10 MBit"), //
	HUNDRED_MBIT(1721, "100 MBit"), //
	NOT_CONNECTED(1725, "Not Connected");

	private final int value;
	private final String name;

	private DataTransferRateOfNetworkTerminalA(int value, String name) {
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