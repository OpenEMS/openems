package io.openems.impl.device.simulator;

import io.openems.api.channel.thingstate.FaultEnum;

public enum FaultEss implements FaultEnum {
	SimulatedError(0);

	private final int value;

	private FaultEss(int value) {
		this.value = value;
	}

	@Override
	public int getValue() {
		return this.value;
	}
}
