package io.openems.edge.pwm;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/*
 * Example JUNit test case
 *
 */

public class PwmModuleTest {

	/*
	 * Example test method
	 */

	@Test
	public void simple() {
		PwmModule impl = new PwmModule();
		assertNotNull(impl);
	}

}
