package io.openems.edge.io.gpio.linuxfs;

import io.openems.edge.io.gpio.api.DigitalIn;

public class LinuxFsDigitalIn extends Gpio implements DigitalIn {

	public LinuxFsDigitalIn(int pinNumber, String basePath) {
		super(pinNumber, Direction.IN, basePath);
	}

	@Override
	public String toString() {
		return super.toString() + ": " + this.getValue().map(t -> t ? "on" : "off").orElse("invalid");
	}
}
