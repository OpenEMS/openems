package io.openems.edge.battery.soltaro.cluster;

import io.openems.edge.battery.api.Settings;

public class ClusterSettings implements Settings {

	private static final double POWER_FACTOR = 0.02;
	private static final int MINIMUM_CURRENT_AMPERE = 1;
	private static final int TOLERANCE_MILLI_VOLT = 10;
	private static final int MAX_INCREASE_MILLIAMPERE = 300;
	
	private int numberOfUsedRacks = 1;
	
	public void setNumberOfUsedRacks(int numberOfUsedRacks) {
		this.numberOfUsedRacks = numberOfUsedRacks;
	}
	
	@Override
	public int getMaxIncreaseMilliAmpere() {
		return MAX_INCREASE_MILLIAMPERE * numberOfUsedRacks;
	}

	@Override
	public double getPowerFactor() {
		return POWER_FACTOR;
	}

	@Override
	public double getMinimumCurrentAmpere() {
		return MINIMUM_CURRENT_AMPERE * numberOfUsedRacks;
	}

	@Override
	public int getToleranceMilliVolt() {
		return TOLERANCE_MILLI_VOLT;
	}
	
}
