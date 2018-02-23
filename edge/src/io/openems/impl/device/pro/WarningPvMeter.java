package io.openems.impl.device.pro;

import io.openems.api.channel.thingstate.WarningEnum;

public enum WarningPvMeter implements WarningEnum {
	NegativePowerL1(0), //
	NegativePowerL2(1), //
	NegativePowerL3(2);

	public final int value;

	private WarningPvMeter(int value) {
		this.value = value;
	}

	@Override
	public int getValue() {
		return this.value;
	}
}
