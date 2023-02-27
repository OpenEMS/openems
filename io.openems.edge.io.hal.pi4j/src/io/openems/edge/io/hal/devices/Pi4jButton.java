package io.openems.edge.io.hal.devices;

import com.pi4j.io.gpio.digital.DigitalInput;

import io.openems.edge.io.hal.api.PressButton;
import io.openems.edge.io.hal.api.ButtonPressCallback;

public class Pi4jButton implements PressButton {

	private final DigitalInput din;

	public Pi4jButton(DigitalInput din, ButtonPressCallback callback) {
		this.din = din;
		this.din.addListener(status -> callback.onButtonPress());
	}

	@Override
	public boolean isPressed() {
		return this.din.isLow();
	}

}
