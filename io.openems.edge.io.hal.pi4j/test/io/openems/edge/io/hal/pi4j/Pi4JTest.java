package io.openems.edge.io.hal.pi4j;

import org.junit.Test;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.plugin.mock.platform.MockPlatform;
import com.pi4j.plugin.mock.provider.gpio.analog.MockAnalogInputProvider;
import com.pi4j.plugin.mock.provider.gpio.analog.MockAnalogOutputProvider;
import com.pi4j.plugin.mock.provider.gpio.digital.MockDigitalOutputProvider;
import com.pi4j.plugin.mock.provider.i2c.MockI2CProvider;
import com.pi4j.plugin.mock.provider.pwm.MockPwmProvider;
import com.pi4j.plugin.mock.provider.serial.MockSerialProvider;
import com.pi4j.plugin.mock.provider.spi.MockSpiProvider;

public class Pi4JTest {

	
	@Test
	public void pi4jTest() {
		Context pi4j = Pi4J.newContextBuilder()
				   .add(new MockPlatform())
				   .add(MockAnalogInputProvider.newInstance(),
				      MockAnalogOutputProvider.newInstance(),
				      MockSpiProvider.newInstance(),
				      MockPwmProvider.newInstance(),
				      MockSerialProvider.newInstance(),
				      MockI2CProvider.newInstance(),
				      MockDigitalOutputProvider.newInstance())
				   .build();
	}
}
