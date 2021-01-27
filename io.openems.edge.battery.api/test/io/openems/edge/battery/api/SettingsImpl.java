package io.openems.edge.battery.api;

import io.openems.edge.battery.api.Settings;

public class SettingsImpl implements Settings {

	private int toleranceMilliVolt;
	private double MinimumCurrentAmpere;
	private double powerFactor;
	private int maxIncreaseMilliAmpere;

	public SettingsImpl(int toleranceMilliVolt, double minimumCurrentAmpere, double powerFactor,
			int maxIncreaseMilliAmpere) {
		super();
		this.toleranceMilliVolt = toleranceMilliVolt;
		MinimumCurrentAmpere = minimumCurrentAmpere;
		this.powerFactor = powerFactor;
		this.maxIncreaseMilliAmpere = maxIncreaseMilliAmpere;
	}

	@Override
	public int getMaxIncreaseMilliAmpere() {
		return this.maxIncreaseMilliAmpere;
	}

	@Override
	public double getPowerFactor() {
		return this.powerFactor;
	}

	@Override
	public double getMinimumCurrentAmpere() {
		return this.MinimumCurrentAmpere;
	}

	@Override
	public int getToleranceMilliVolt() {
		return this.toleranceMilliVolt;
	}

}
