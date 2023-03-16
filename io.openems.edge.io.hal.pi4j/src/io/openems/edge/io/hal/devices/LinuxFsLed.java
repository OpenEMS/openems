package io.openems.edge.io.hal.devices;

import io.openems.edge.io.hal.api.Led;
import io.openems.edge.io.hal.linuxfs.Direction;
import io.openems.edge.io.hal.linuxfs.LinuxFsDigitalOut;

public class LinuxFsLed implements Led {

	private final LinuxFsDigitalOut dout;
	
	public LinuxFsLed(int pinNumber) {
		this.dout = new LinuxFsDigitalOut(pinNumber);
	}
	
	@Override
	public void on() {
		this.dout.on();
	}

	@Override
	public void off() {
		this.dout.off();
	}

	@Override
	public void toggle() {
		this.dout.toggle();
		
	}

	@Override
	public boolean isOn() {
		return this.dout.isOn();
	}

}
