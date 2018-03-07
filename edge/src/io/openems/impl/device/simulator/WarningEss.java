package io.openems.impl.device.simulator;

import io.openems.api.channel.thingstate.WarningEnum;
import io.openems.common.types.ThingStateInfo;

@ThingStateInfo(reference = SimulatorAsymmetricEss.class)
public enum WarningEss implements WarningEnum {
	SimulatedWarning(0);

	private final int value;

	private WarningEss(int value) {
		this.value = value;
	}

	@Override
	public int getValue() {
		return this.value;
	}
}
