package io.openems.edge.io.hal.devices;

import io.openems.edge.io.hal.api.Led;
import io.openems.edge.io.hal.api.PressButton;
import io.openems.edge.io.hal.linuxfs.Direction;
import io.openems.edge.io.hal.linuxfs.LinuxFsDigitalIn;
import io.openems.edge.io.hal.linuxfs.LinuxFsDigitalOut;

public class LinuxFsButton implements PressButton {

	private final LinuxFsDigitalIn din;
	
	public LinuxFsButton(int pinNumber) {
		this.din = new LinuxFsDigitalIn(pinNumber);
	}

	@Override
	public boolean isPressed() {
		return this.din.getValue();
	}
	

}
