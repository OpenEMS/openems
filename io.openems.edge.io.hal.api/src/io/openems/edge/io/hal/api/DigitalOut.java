package io.openems.edge.io.hal.api;

public interface DigitalOut {
	boolean isOn();
	boolean isOff();

	void toggle();
	void setOn();
	void setOff();
}
