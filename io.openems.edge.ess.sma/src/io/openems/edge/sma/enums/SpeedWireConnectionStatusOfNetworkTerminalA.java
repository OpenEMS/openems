package io.openems.edge.sma.enums;

import io.openems.common.types.OptionsEnum;

public enum SpeedWireConnectionStatusOfNetworkTerminalA implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	ALARM(35, "Alarm"), //
	OK(307, "OK"), //
	WARNING(455, "Warning"), //
	NOT_CONNECTED(1725, "Not Connected"); //

	private final int value;
	private final String name;

	private SpeedWireConnectionStatusOfNetworkTerminalA(int value, String name) {
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