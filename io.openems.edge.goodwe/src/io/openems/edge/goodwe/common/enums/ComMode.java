package io.openems.edge.goodwe.common.enums;

import io.openems.common.types.OptionsEnum;

public enum ComMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	WIFI(1, "Wifi"), //
	GPRS(2, "gprs"), //
	LAN(3, "Lan e20"), //
	WIFI_A21(4, "Wifi mode of Wifi+Lan module"), //
	LAN_A21(5, "Lan mode of Wifi+Lan module");

	private final int value;
	private final String option;

	private ComMode(int value, String option) {
		this.value = value;
		this.option = option;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.option;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}