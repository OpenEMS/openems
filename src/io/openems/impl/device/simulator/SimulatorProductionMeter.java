package io.openems.impl.device.simulator;

import io.openems.api.exception.ConfigException;

public class SimulatorProductionMeter extends SimulatorMeter {

	public SimulatorProductionMeter(String thingId) throws ConfigException {
		super(thingId);
	}

	@Override
	protected long getMinApparentPower() {
		return 0;
	}

	@Override
	protected long getMaxApparentPower() {
		return +10000;
	}

	@Override
	protected double getMinCosPhi() {
		return 0.9;
	}

	@Override
	protected double getMaxCosPhi() {
		return 1.1;
	}
}
