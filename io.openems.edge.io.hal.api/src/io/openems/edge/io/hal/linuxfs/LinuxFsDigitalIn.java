package io.openems.edge.io.hal.linuxfs;

public class LinuxFsDigitalIn extends Gpio {
	
	public LinuxFsDigitalIn(int pinNumber, String basePath) {
		super(pinNumber, Direction.IN, basePath);
	}

	public boolean getValue() {
		return super.getValue();
	}
	
	public boolean isOn() {
		return this.getValue();
	}
}
