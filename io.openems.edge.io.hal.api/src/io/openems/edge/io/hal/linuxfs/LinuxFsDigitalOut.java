package io.openems.edge.io.hal.linuxfs;

import io.openems.edge.io.hal.api.DigitalOut;

public class LinuxFsDigitalOut extends Gpio implements DigitalOut {
	
	public LinuxFsDigitalOut(int pinNumber, String basePath) {
		super(pinNumber, Direction.OUT, basePath);
	}

	/**
	 * Sets the value of the output.
	 * @param value true if the output should be low, false otherwise.
	 */
	public void setValue(boolean value) {
		String writeVal;
		if (value) {
			writeVal = "1";
		} else {
			writeVal = "0";
		}
		writeValue(writeVal);
	}
	
	/**
	 * Reads the value of the output.
	 * This method does not use caching and reads the value from the hardware.
	 * @return true if the output has HIGH state, otherwise false.
	 */
	public boolean getValue() {
		return super.getValue();
	}
	
	/**
	 * Sets the digital output to HIGH state.
	 */
	public void on() {
		this.setValue(true);
	}
	
	/**
	 * Sets the digital output to LOW state.
	 */
	public void off() {
		this.setValue(false);
	}
	
	/**
	 * Inverts the digital output state.
	 */
	public void toggle() {
		var current = this.getValue();
		this.setValue(!current);
	}
	
	public boolean isOn() {
		return this.getValue();
	}

	@Override
	public boolean isOff() {
		return this.getValue() == false;
	}

	@Override
	public void setOn() {
		this.on();
	}

	@Override
	public void setOff() {
		this.off();
	}
	
	@Override
	public void release() {
		try {
			this.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public String toString() {
		if (this.isOn())
			return "on";
		else 
			return "off";
	}

}
