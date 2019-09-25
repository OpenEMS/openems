package io.openems.edge.sensors.temperature.mcp3208;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/*
 * Example JUNit test case
 *
 */

public class Mcp3208TemperatureSensorTest {

	/*
	 * Example test method
	 */

	@Test
	public void simple() {
		Mcp3208TemperatureSensor impl = new Mcp3208TemperatureSensor();
		assertNotNull(impl);
	}

}
