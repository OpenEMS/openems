package io.openems.edge.raspberrypi.spi;

import static org.junit.Assert.assertNotNull;

import io.openems.edge.common.component.AbstractOpenemsComponent;
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
		AbstractOpenemsComponent impl = new SpiInitialImpl();
		assertNotNull(impl);
	}

}
