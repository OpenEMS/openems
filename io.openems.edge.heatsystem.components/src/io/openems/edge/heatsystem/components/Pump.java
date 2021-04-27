package io.openems.edge.heatsystem.components;

public interface Pump extends PassingForPid {

	boolean readyToChange();

	boolean changeByPercentage(double percentage);

	void controlRelays(boolean activate, String whichRelays);

	void setPowerLevel(double percent);

}
