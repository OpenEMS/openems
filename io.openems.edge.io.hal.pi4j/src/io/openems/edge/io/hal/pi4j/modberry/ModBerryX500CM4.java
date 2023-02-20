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
	
	public Led getLed(Cm4Hardware.Led led) {
		var handle = this.pi4j.digitalOutput().create(DigitalOutputConfigBuilder.newInstance(pi4j)
				.address(led.getGpio())
				.id("User LED " + led.toString())
				.initial(DigitalState.LOW)
				.build());
		
		return new Pi4jLed(handle);
	}
	
	public PressButton getButton(Cm4Hardware.Button btn, ButtonPressCallback callback) {
		var handle = this.pi4j.digitalInput().create(DigitalInputConfigBuilder.newInstance(pi4j)
				.address(btn.getGpio())
				.id("User Button " + btn.toString())
				.debounce(100L)
				.build());
		return new Pi4jButton(handle, callback);
	}
}
