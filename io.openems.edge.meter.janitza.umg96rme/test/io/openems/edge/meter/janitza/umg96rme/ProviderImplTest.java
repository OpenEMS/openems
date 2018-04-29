package io.openems.edge.meter.janitza.umg96rme;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.edge.meter.janitza.umg96rme.MeterJanitzaUmg96rme;

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
		MeterJanitzaUmg96rme impl = new MeterJanitzaUmg96rme();
		assertNotNull(impl);
	}

}
