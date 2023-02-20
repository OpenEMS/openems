package io.openems.edge.io.hal.pi4j.pi4j;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.pi4j.io.gpio.digital.DigitalOutput;

public class DigitalOutputTest extends AbstractHardwareTest {
	
    protected DigitalOutput digitalOutput;

    @Before
    public void setUp() {
        this.digitalOutput = pi4j.digitalOutput().create(0x0, "", "Example DIO");
    }
    
    @Test
    public void testDigitalOutputToggle() {
    	digitalOutput.setState(false);
    	assertTrue(digitalOutput.isOff());
    	digitalOutput.on();
    	assertTrue(digitalOutput.isOn());
    }
}
