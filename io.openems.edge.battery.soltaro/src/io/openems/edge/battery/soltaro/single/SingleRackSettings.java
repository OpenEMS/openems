package io.openems.edge.battery.soltaro.single;

import io.openems.edge.battery.api.Settings;

public class SingleRackSettings implements Settings {

	private static final double POWER_FACTOR = 0.02;
	private static final int MINIMUM_CURRENT_AMPERE = 1;
	private static final int TOLERANCE_MILLI_VOLT = 10;
	private static final int MAX_INCREASE_MILLIAMPERE = 300;
	
	@Override
	public int getMaxIncreaseMilliAmpere() {
		return MAX_INCREASE_MILLIAMPERE;
	}

	@Override
	public double getPowerFactor() {
		return POWER_FACTOR;
	}

	@Override
	public double getMinimumCurrentAmpere() {
		return MINIMUM_CURRENT_AMPERE;
	}

	@Override
	public int getToleranceMilliVolt() {
		return TOLERANCE_MILLI_VOLT;
	}

}
