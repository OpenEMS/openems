package io.openems.edge.meter.weidmuller;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.edge.meter.weidmuller.MeterWeidmuller525;

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
		MeterWeidmuller525 impl = new MeterWeidmuller525();
		assertNotNull(impl);
	}

}
