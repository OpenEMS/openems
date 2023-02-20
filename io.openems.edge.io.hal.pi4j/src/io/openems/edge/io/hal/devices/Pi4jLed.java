package io.openems.edge.io.hal.devices;

import java.util.concurrent.TimeUnit;

import com.pi4j.io.gpio.digital.DigitalOutput;

import io.openems.edge.io.hal.api.Led;

public class Pi4jLed implements Led {

	private final DigitalOutput dout; 
	
	public Pi4jLed(DigitalOutput dout) {
		this.dout = dout;
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
		return this.dout.isHigh();
	}

	@Override
	public void blink(int millis) {
		this.dout.blink(millis, TimeUnit.MILLISECONDS);
	}
	
	@Override
	public void pulse(int millis) {
		this.dout.pulse(millis, TimeUnit.MILLISECONDS);
	}

}
