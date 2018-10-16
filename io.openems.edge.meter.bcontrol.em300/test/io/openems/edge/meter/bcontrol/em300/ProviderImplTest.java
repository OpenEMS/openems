package io.openems.edge.meter.bcontrol.em300;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.edge.meter.bcontrol.em300.MeterBControlEM300;

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
		MeterBControlEM300 impl = new MeterBControlEM300();
		assertNotNull(impl);
	}

}
