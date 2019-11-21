package io.openems.edge.temperature.sensor;

import static org.junit.Assert.assertNotNull;

import io.openems.edge.temperature.sensor.temperaturesensor.TemperatureSensorImpl;
import org.junit.Test;

/*
 * Example JUNit test case
 *
 */

public class TemperatureSensorImplTest {

	/*
	 * Example test method
	 */

	@Test
	public void simple() {
		TemperatureSensorImpl impl = new TemperatureSensorImpl();
		assertNotNull(impl);
	}

}
