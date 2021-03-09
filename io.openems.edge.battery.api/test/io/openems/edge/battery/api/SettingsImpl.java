package io.openems.edge.battery.api;

public class SettingsImpl implements Settings {

	private int toleranceMilliVolt;
	private double minimumCurrentAmpere;
	private double powerFactor;
	private int maxIncreaseMilliAmpere;

	public SettingsImpl(int toleranceMilliVolt, double minimumCurrentAmpere, double powerFactor,
			int maxIncreaseMilliAmpere) {
		super();
		this.toleranceMilliVolt = toleranceMilliVolt;
		this.minimumCurrentAmpere = minimumCurrentAmpere;
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
		return this.minimumCurrentAmpere;
	}

	@Override
	public int getToleranceMilliVolt() {
		return this.toleranceMilliVolt;
	}

}
