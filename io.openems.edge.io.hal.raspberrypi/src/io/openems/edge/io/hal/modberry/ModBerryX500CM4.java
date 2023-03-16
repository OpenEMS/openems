package io.openems.edge.io.hal.modberry;

import io.openems.edge.io.hal.api.DigitalOut;
import io.openems.edge.io.hal.api.Led;
import io.openems.edge.io.hal.api.PressButton;
import io.openems.edge.io.hal.devices.LinuxFsButton;
import io.openems.edge.io.hal.devices.LinuxFsLed;
import io.openems.edge.io.hal.linuxfs.HardwareFactory;

public class ModBerryX500CM4 {
	
	private HardwareFactory context;
	
	public ModBerryX500CM4(HardwareFactory context) {
		// TODO take care about double initializing pins.
		this.context = context;
	}

	/**
	 * Gets one of the Modberry LEDs.
	 * @param led to select which LED you want to control.
	 * @return a LED object ready for blinking.
	 */
	public Led getLed(Cm4Hardware.Led led) {
		return new LinuxFsLed(context, led.getGpio());
	}

	/**
	 * Creates the built-in button of the Modberry X500.
	 * For each button it can be queried if it is currently pressed or the caller can register a callback
	 * for getting notified on button press.
	 * @param btn specifies which button you want.
	 * @param callback gets called every time the button is pressed.
	 * @return a PressButton object ready to use.
	 */
	public PressButton getButton(Cm4Hardware.Button btn) {
		// Creating the button via the absctaction layer.
		return new LinuxFsButton(context, btn.getGpio());
	}

	
	public DigitalOut getDigitalOut(Cm4Hardware.DigitalOut pin) {
		// Creating the outputs directly with the low level API.
		return context.fabricateOut(pin.getGpio());
	}
	
	public DigitalOut getDigitalOut(Cm4Hardware.BidirectionalIo pin) {
		return context.fabricateOut(pin.getGpio());
	}
}
