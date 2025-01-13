package io.openems.edge.io.gpio.linuxfs;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.io.gpio.api.DigitalOut;

public class LinuxFsDigitalOut extends Gpio implements DigitalOut {

	public LinuxFsDigitalOut(int pinNumber, String basePath) {
		super(pinNumber, Direction.OUT, basePath);
	}

	/**
	 * Sets the value of the output.
	 * 
	 * @param value true if the output should be low, false otherwise.
	 * @throws OpenemsException if the value could not be written to the device.
	 */
	public void setValue(boolean value) throws OpenemsException {
		this.writeValue(value ? "1" : "0");
	}

	@Override
	public String toString() {
		return this.getValue().map(t -> t ? "on" : "off").orElse("error");
	}
}
