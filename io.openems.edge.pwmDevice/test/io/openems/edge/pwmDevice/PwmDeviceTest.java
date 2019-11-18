package io.openems.edge.pwmDevice;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/*
 * Example JUNit test case
 *
 */

public class PwmDeviceTest {

	/*
	 * Example test method
	 */

	@Test
	public void simple() {
		PwmDevice impl = new PwmDevice();
		assertNotNull(impl);
	}

}
