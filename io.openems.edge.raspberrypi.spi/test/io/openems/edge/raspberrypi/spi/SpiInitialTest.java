package io.openems.edge.raspberrypi.spi;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/*
 * Example JUNit test case
 *
 */

public class SpiInitialTest {

	/*
	 * Example test method
	 */

	@Test
	public void simple() {
		SpiInitial impl = new SpiInitial();
		assertNotNull(impl);
	}

}
