package io.openems.edge.bridgei2c;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/*
 * Example JUNit test case
 *
 */

public class I2cBridgeImplTest {

	/*
	 * Example test method
	 */

	@Test
	public void simple() {
		I2cBridgeImpl impl = new I2cBridgeImpl();
		assertNotNull(impl);
	}

}
