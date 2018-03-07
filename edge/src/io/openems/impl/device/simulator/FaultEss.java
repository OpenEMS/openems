package io.openems.impl.device.simulator;

import io.openems.api.channel.thingstate.FaultEnum;
import io.openems.common.types.ThingStateInfo;

@ThingStateInfo(reference = SimulatorAsymmetricEss.class)
public enum FaultEss implements FaultEnum {
	SimulatedFault(0);

	private final int value;

	private FaultEss(int value) {
		this.value = value;
	}

	@Override
	public int getValue() {
		return this.value;
	}
}
