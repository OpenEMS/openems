package io.openems.edge.battery.api;

public interface Settings {

	int getMaxIncreaseMilliAmpere();

	double getPowerFactor();

	double getMinimumCurrentAmpere();

	int getToleranceMilliVolt();

}