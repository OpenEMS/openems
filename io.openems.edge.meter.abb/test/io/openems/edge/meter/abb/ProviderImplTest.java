package io.openems.edge.meter.abb;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.edge.meter.abb.b32.MeterABBB23Mbus;

/*
 * Example JUNit test case
 *
 */

public class ProviderImplTest {

	/*
	 * Example test method
	 */

	@Test
	public void simple() {
		MeterABBB23Mbus impl = new MeterABBB23Mbus();
		assertNotNull(impl);
	}

}
