package io.openems.edge.io.hal.modberry;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.io.hal.api.DigitalIn;
import io.openems.edge.io.hal.api.DigitalOut;
import io.openems.edge.io.hal.api.HardwareComponent;
import io.openems.edge.io.hal.api.Led;
import io.openems.edge.io.hal.api.PressButton;
import io.openems.edge.io.hal.api.linuxfs.devices.LinuxFsButton;
import io.openems.edge.io.hal.api.linuxfs.devices.LinuxFsLed;
import io.openems.edge.io.hal.linuxfs.HardwareFactory;

public class ModBerryX500CM4 extends RaspberryPiPlattform {
	
	private Logger logger = LoggerFactory.getLogger(ModBerryX500CM4.class);
	
	private HardwareFactory context;
	private Map<Integer, HardwareComponent> occupiedPins;
	
	public ModBerryX500CM4(HardwareFactory context) {
		this.context = context;
		this.occupiedPins = new HashMap<>();
	}

	/**
	 * Gets one of the Modberry LEDs.
	 * @param led to select which LED you want to control.
	 * @return a LED object ready for blinking.
	 */
	public Led getLed(ModberryX500CM4Hardware.Led led) {
		var hardwareLed = new LinuxFsLed(this.context, led.getGpio());
		this.lockComponent(led.getGpio(), hardwareLed);
		return hardwareLed;
	}

	/**
	 * Creates the built-in button of the Modberry X500.
	 * For each button it can be queried if it is currently pressed or the caller can register a callback
	 * for getting notified on button press.
	 * @param btn specifies which button you want.
	 * @return a PressButton object ready to use.
	 */
	public PressButton getButton(ModberryX500CM4Hardware.Button btn) {
		// Creating the button via the abstraction layer.
		var hardwareButton = new LinuxFsButton(this.context, btn.getGpio());
		this.lockComponent(btn.getGpio(), hardwareButton);
		return hardwareButton;
	}

	/**
	 * Creates a digital output on the Modberry X500 on the output-only ports.
	 * @param pin the pin which the output should occupy.
	 * @return digital output object.
	 */
	public DigitalOut getDigitalOut(ModberryX500CM4Hardware.DigitalOut pin) {
		// Creating the outputs directly with the low level API.
		var dout = this.context.fabricateOut(pin.getGpio());
		this.lockComponent(pin.getGpio(), dout);
		return dout;
	}
	
	/**
	 * Creates a digital output from the bidirectional IO ports of the Modberry X500.
	 * @param pin the pin which should be occupied.
	 * @return digital output object.
	 */
	public DigitalOut getDigitalOut(ModberryX500CM4Hardware.BidirectionalIo pin) {
		var dout = this.context.fabricateOut(pin.getGpio());
		this.lockComponent(pin.getGpio(), dout);
		return dout;
	}
	
	/**
	 * Creates a digital input from one of the opto isolated inputs of the Modberry X500.
	 * @param pin the pin which should be occupied.
	 * @return digital input object.
	 */
	public DigitalIn getDigitalIn(ModberryX500CM4Hardware.OptoDigitalIn pin) {
		var din = this.context.fabricateIn(pin.getGpio());
		this.lockComponent(pin.getGpio(), din);
		return din;
	}
	
	/**
	 * Releases the component.
	 * @param pinNumber the pin number of the component.
	 */
	public void releaseComponent(int pinNumber) {
		this.occupiedPins.remove(pinNumber).release();
	}
	
	private void lockComponent(int pinNumber, HardwareComponent comp) {
		if (this.occupiedPins.containsKey(pinNumber)) {
			this.logger.error("Pin " + pinNumber + " already in use!");
		}
		this.occupiedPins.put(pinNumber, comp);
	}
}
