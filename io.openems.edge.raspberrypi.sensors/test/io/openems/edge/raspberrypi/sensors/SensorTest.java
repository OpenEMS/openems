package io.openems.edge.raspberrypi.sensors;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/*
 * Example JUNit test case
 *
 */

public class SensorTest {

	/*
	 * Example test method
	 */

	@Test
	public void simple() {
		Sensor impl = new Sensor();
		assertNotNull(impl);
	}

}
