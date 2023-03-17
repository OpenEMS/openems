package io.openems.edge.io.hal.api;

public interface DigitalOut extends HardwareComponent {
	boolean isOn();
	boolean isOff();

	void toggle();
	void setOn();
	void setOff();
}
