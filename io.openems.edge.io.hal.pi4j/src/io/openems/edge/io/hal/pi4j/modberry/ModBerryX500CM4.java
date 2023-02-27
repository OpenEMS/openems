package io.openems.edge.io.hal.pi4j.modberry;

import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.DigitalInputConfigBuilder;
import com.pi4j.io.gpio.digital.DigitalOutputConfigBuilder;
import com.pi4j.io.gpio.digital.DigitalState;

import io.openems.edge.io.hal.api.ButtonPressCallback;
import io.openems.edge.io.hal.api.Led;
import io.openems.edge.io.hal.api.PressButton;
import io.openems.edge.io.hal.devices.Pi4jButton;
import io.openems.edge.io.hal.devices.Pi4jLed;

public class ModBerryX500CM4 {
	
	private Context pi4j;
	
	public ModBerryX500CM4(Context pi4j) {
		this.pi4j = pi4j;
	}

	/**
	 * Gets one of the Modberry LEDs.
	 * @param led to select which LED you want to control.
	 * @return a LED object ready for blinking.
	 */
	public Led getLed(Cm4Hardware.Led led) {
		var handle = this.pi4j.digitalOutput().create(DigitalOutputConfigBuilder.newInstance(this.pi4j)
				.address(led.getGpio())
				.id("User LED " + led.toString())
				.initial(DigitalState.LOW)
				.build());
		
		return new Pi4jLed(handle);
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
		var handle = this.pi4j.digitalInput().create(DigitalInputConfigBuilder.newInstance(this.pi4j)
				.address(btn.getGpio())
				.id("User Button " + btn.toString())
				.debounce(100L)
				.build());
		return new Pi4jButton(handle, callback);
	}
}
