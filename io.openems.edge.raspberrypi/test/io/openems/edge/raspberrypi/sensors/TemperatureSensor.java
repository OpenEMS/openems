package io.openems.edge.raspberrypi.sensors;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/*
 * Example JUNit test case
 *
 */

public class TemperatureSensor {

	/*
	 * Example test method
	 */

	@Test
	public void simple() {
		TemperatureSensor impl = new TemperatureSensor();
		assertNotNull(impl);
	}

}
