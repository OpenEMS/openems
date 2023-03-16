package io.openems.edge.io.hal.linuxfs;

import io.openems.edge.io.hal.api.DigitalOut;

public class LinuxFsDigitalOut extends Gpio implements DigitalOut {
	
	public LinuxFsDigitalOut(int pinNumber, String basePath) {
		super(pinNumber, Direction.OUT, basePath);
	}

	public void setValue(boolean value) {
		String writeVal;
		if(value) {
			writeVal = "1";
		} else {
			writeVal = "0";
		}
		writeValue(writeVal);
	}
	
	public boolean getValue() {
		return super.getValue();
	}
	
	public void on() {
		this.setValue(true);
	}
	
	public void off() {
		this.setValue(false);
	}
	
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
}
