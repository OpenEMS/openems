package io.openems.edge.io.hal.modberry;

import io.openems.edge.io.hal.api.ButtonPressCallback;
import io.openems.edge.io.hal.api.Led;
import io.openems.edge.io.hal.api.PressButton;
import io.openems.edge.io.hal.devices.LinuxFsButton;
import io.openems.edge.io.hal.devices.LinuxFsLed;

public class ModBerryX500CM4 {
	
	public ModBerryX500CM4() {
		
	}

	/**
	 * Gets one of the Modberry LEDs.
	 * @param led to select which LED you want to control.
	 * @return a LED object ready for blinking.
	 */
	public Led getLed(Cm4Hardware.Led led) {

		return new LinuxFsLed(led.getGpio());
	}

	/**
	 * Creates the built-in button of the Modberry X500.
	 * For each button it can be queried if it is currently pressed or the caller can register a callback
	 * for getting notified on button press.
	 * @param btn specifies which button you want.
	 * @param callback gets called every time the button is pressed.
	 * @return a PressButton object ready to use.
	 */
	public PressButton getButton(Cm4Hardware.Button btn, ButtonPressCallback callback) {
		return new LinuxFsButton(btn.getGpio());
	}

}
