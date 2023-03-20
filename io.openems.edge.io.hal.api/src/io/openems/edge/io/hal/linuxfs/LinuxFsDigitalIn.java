package io.openems.edge.io.hal.linuxfs;

import io.openems.edge.io.hal.api.DigitalIn;

public class LinuxFsDigitalIn extends Gpio implements DigitalIn {
	
	public LinuxFsDigitalIn(int pinNumber, String basePath) {
		super(pinNumber, Direction.IN, basePath);
	}

	public boolean getValue() {
		return super.getValue();
	}
	
	public boolean isOn() {
		return this.getValue();
	}

	@Override
	public boolean isOff() {
		return !this.getValue();
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
