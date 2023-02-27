package io.openems.edge.io.hal.pi4j.pi4j;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.i2c.I2C;
import com.pi4j.io.pwm.Pwm;
import com.pi4j.io.spi.Spi;
import com.pi4j.plugin.mock.platform.MockPlatform;
import com.pi4j.plugin.mock.provider.gpio.analog.MockAnalogInputProvider;
import com.pi4j.plugin.mock.provider.gpio.analog.MockAnalogOutputProvider;
import com.pi4j.plugin.mock.provider.gpio.digital.MockDigitalInput;
import com.pi4j.plugin.mock.provider.gpio.digital.MockDigitalInputProvider;
import com.pi4j.plugin.mock.provider.gpio.digital.MockDigitalOutput;
import com.pi4j.plugin.mock.provider.gpio.digital.MockDigitalOutputProvider;
import com.pi4j.plugin.mock.provider.i2c.MockI2C;
import com.pi4j.plugin.mock.provider.i2c.MockI2CProvider;
import com.pi4j.plugin.mock.provider.pwm.MockPwm;
import com.pi4j.plugin.mock.provider.pwm.MockPwmProvider;
import com.pi4j.plugin.mock.provider.serial.MockSerialProvider;
import com.pi4j.plugin.mock.provider.spi.MockSpi;
import com.pi4j.plugin.mock.provider.spi.MockSpiProvider;

public abstract class AbstractHardwareTest {
	
	protected Context pi4j;
	
	@Before
	public final void setUpBase() throws Exception {
		this.pi4j = Pi4J.newContextBuilder()
				.add(new MockPlatform())
				.add(
						MockAnalogInputProvider.newInstance(),
						MockAnalogOutputProvider.newInstance(),
						MockSpiProvider.newInstance(),
						MockSpiProvider.newInstance(),
						MockPwmProvider.newInstance(),
						MockSerialProvider.newInstance(),
						MockI2CProvider.newInstance(),
						MockDigitalInputProvider.newInstance(),
						MockDigitalOutputProvider.newInstance())
				.build();
	}
	
	@After
	public final void tearDownBase() throws Exception {
		this.pi4j.shutdown();
	}
	
	protected MockDigitalInput toMock(DigitalInput digitalInput) {
		return this.toMock(MockDigitalInput.class, digitalInput);
	}
	
    protected MockDigitalOutput toMock(DigitalOutput digitalOutput) {
        return this.toMock(MockDigitalOutput.class, digitalOutput);
    }

    protected MockDigitalOutput[] toMock(DigitalOutput[] digitalOutputs) {
        return Arrays.stream(digitalOutputs).map(this::toMock).toArray(MockDigitalOutput[]::new);
    }

    protected MockPwm toMock(Pwm pwm) {
        return this.toMock(MockPwm.class, pwm);
    }

    protected MockI2C toMock(I2C i2c) {
        return this.toMock(MockI2C.class, i2c);
    }

    protected MockSpi toMock(Spi spi) {
        return this.toMock(MockSpi.class, spi);
    }
	
    private <T> T toMock(Class<T> type, Object instance) {
        return type.cast(instance);
    }
}
