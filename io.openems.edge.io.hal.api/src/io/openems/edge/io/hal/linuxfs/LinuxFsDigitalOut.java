package io.openems.edge.io.hal.linuxfs;

public class LinuxFsDigitalOut extends Gpio {
	
	public LinuxFsDigitalOut(int pinNumber) {
		super(pinNumber, Direction.OUT);
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
}
