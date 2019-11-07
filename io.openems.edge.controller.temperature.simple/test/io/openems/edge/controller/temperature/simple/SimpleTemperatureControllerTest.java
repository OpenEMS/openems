package io.openems.edge.controller.temperature.simple;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/*
 * Example JUNit test case
 *
 */

public class SimpleTemperatureControllerTest {

	/*
	 * Example test method
	 */

	@Test
	public void simple() {
		SimpleTemperatureController impl = new SimpleTemperatureController();
		assertNotNull(impl);
	}

}
