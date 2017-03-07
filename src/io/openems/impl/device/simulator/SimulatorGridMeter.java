package io.openems.impl.device.simulator;

import io.openems.api.exception.ConfigException;

public class SimulatorGridMeter extends SimulatorMeter {

	public SimulatorGridMeter(String thingId) throws ConfigException {
		super(thingId);
	}

	@Override
	protected long getMinApparentPower() {
		return -10000;
	}

	@Override
	protected long getMaxApparentPower() {
		return +10000;
	}

	@Override
	protected double getMinCosPhi() {
		return -1.5;
	}

	@Override
	protected double getMaxCosPhi() {
		return 1.5;
	}
}
